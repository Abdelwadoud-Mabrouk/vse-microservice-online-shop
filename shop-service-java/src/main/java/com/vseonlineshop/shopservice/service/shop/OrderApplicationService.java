package com.vseonlineshop.shopservice.service.shop;

import com.vseonlineshop.shopservice.domain.*;
import com.vseonlineshop.shopservice.dto.order.response.*;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.dto.order.request.OrderRequest;
import com.vseonlineshop.shopservice.dto.warehouse.*;
import com.vseonlineshop.shopservice.exception.*;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import com.vseonlineshop.shopservice.event.producer.KafkaEventPublisher;
import com.vseonlineshop.shopservice.service.client.BillingService;
import com.vseonlineshop.shopservice.service.client.WarehouseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static com.vseonlineshop.shopservice.config.LogUtils.safeContext;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderTransactionHelper orderTransactionHelper;
    private final WarehouseClient warehouseClient;
    private final BillingService paymentClient;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final OrderServiceTool toolService;


    // ── Create Order ─────────────────────────────────────────────────────────
    public CheckoutResponse createOrder(OrderRequest request, UUID idempotencyKey) {

        long start = System.currentTimeMillis();

        Optional<Order> existingOrderOpt = orderRepository.findByIdempotencyKey(idempotencyKey);

        if (existingOrderOpt.isPresent()) {
            Order existingOrder = existingOrderOpt.get();

            if (existingOrder.getStatus() == OrderStatus.PENDING
                    || existingOrder.getStatus() == OrderStatus.PROCESSING) {
                log.warn("Order wird bereits verarbeitet, Duplikat-Request abgelehnt",
                        kv("correlationId", existingOrder.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext("idempotencyKey", idempotencyKey)));

                throw new BusinessException(
                        "Bestellung wird bereits verarbeitet. Bitte warten.", HttpStatus.CONFLICT
                );
            }

            log.info("Duplikat-Request erkannt, gebe bestehende Order zurück",
                    kv("correlationId", existingOrder.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", safeContext("orderId", existingOrder.getId(), "status", existingOrder.getStatus())));

            return toolService.mapToCheckoutResponse(existingOrder);
        }

        Order order;
        UUID correlationId = UUID.randomUUID();

        try {
            order = orderTransactionHelper.createInitialOrder(request, correlationId, idempotencyKey);
            log.info(
                    "Order erfolgreich angelegt",
                    kv("correlationId", order.getCorrelationId()),
                    kv("idempotencyKey", idempotencyKey),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", order.getId(),
                            "status", order.getStatus()
                    ))
            );
            Order finalOrder = order;
            toolService.safePublish(() -> kafkaEventPublisher.publishOrderCreated(finalOrder));

        } catch (DataIntegrityViolationException dupEx) {
            log.warn("Race Condition bei idempotencyKey erkannt, lade Ergebnis des parallelen Requests",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext("idempotencyKey", idempotencyKey)));

            throw new BusinessException("Bestellung wird bereits verarbeitet.", HttpStatus.BAD_REQUEST);


        } catch (Exception ex) {
            log.error(
                    "Konnte initiale Order nicht in DB anlegen",
                    kv("correlationId", correlationId),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "errorCode", "CREATION_ORDER_FAILED",
                            "errorMessage", ex.getMessage()
                    )));
            toolService.cancelOrder(null, null, OrderStatus.CANCELLED, "CREATION_ORDER_FAILED", ex);
            throw new GlobalException("DATABASE_ERROR", "Bestellung konnte nicht angelegt werden.");
        }

        ReservationResponse reservation = null;

        try {
            // Warehouse reservieren
            try {
                reservation = warehouseClient.reserve(order);
            } catch (WarehouseServiceException ex) {
                // 4xx Fehler
                log.warn(
                        "Bestand reicht nicht fuer Order",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext(
                                "orderId", order.getId(),
                                "errorMessage", ex.getMessage()
                        ))
                );

                toolService.cancelOrder(order, null, OrderStatus.OUT_OF_STOCK, "OUT_OF_STOCK", ex);
                throw new BusinessException("OUT_OF_STOCK", HttpStatus.CONFLICT);

            } catch (ResourceAccessException | HttpServerErrorException ex) {
                // 5xx Server Error oder Timeout/Connection Refused: Service wirklich DOWN
                String errorMessage = (ex.getMostSpecificCause() != null)
                        ? ex.getMostSpecificCause().getMessage()
                        : ex.getMessage();
                log.error(
                        "Lager-Service nicht erreichbar",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext(
                                "orderId", order.getId(),
                                "errorMessage", errorMessage != null ? errorMessage : "Verbindung fehlgeschlagen (Connection refused/Timeout)"
                        ))
                );

                toolService.cancelOrder(order, null, OrderStatus.CANCELLED, "WAREHOUSE_SERVICE_UNAVAILABLE", ex);
                throw new ServiceUnavailableException("WAREHOUSE_SERVICE_UNAVAILABLE", "Lager-Service derzeit nicht erreichbar.");
            }

            if (reservation == null || reservation.items() == null || reservation.items().isEmpty()) {
                log.error(
                        "Warehouse lieferte eine leere Reservierung",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext(
                                "orderId", order.getId()
                        ))
                );
                toolService.cancelOrder(order, null, OrderStatus.CANCELLED, "EMPTY_RESERVATION", null);
                throw new BusinessException("EMPTY_RESERVATION", HttpStatus.BAD_REQUEST);
            }

            // Save ReservationId in order Tabelle (needed to commit reservation)
            order = orderTransactionHelper.updateToProcessing(order, reservation.reservationId());

            //  Payment aufrufen
            PaymentResponse payment;
            try {
                payment = paymentClient.pay(order);
            } catch (HttpClientErrorException ex) {
                // 4xx
                log.warn(
                        "Payment Request vom Billing-Service abgelehnt",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext(
                                "orderId", order.getId(),
                                "reservationId", reservation.reservationId(),
                                "httpStatus", ex.getStatusCode().value(),
                                "errorMessage", ex.getMessage()
                        ))
                );

                // Rollback durchführen (Lager freigeben)
                toolService.cancelOrder(order, reservation.reservationId(), OrderStatus.PAYMENT_FAILED, "INVALID_PAYMENT_REQUEST", ex);

                throw new BusinessException("INVALID_PAYMENT_REQUEST", HttpStatus.PAYMENT_REQUIRED);

            } catch (ResourceAccessException | HttpServerErrorException ex) {
                // 5xx / Timeout: Nach 4 Retries immer noch offline/down
                log.error(
                        "Payment-Service nach allen Retry-Versuchen nicht erreichbar",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", safeContext(
                                "orderId", order.getId(),
                                "status", order.getStatus(),
                                "errorMessage", ex.getMessage()
                        ))
                );

                toolService.cancelOrder(order, reservation.reservationId(), OrderStatus.PAYMENT_FAILED, "PAYMENT_SERVICE_UNAVAILABLE", ex);

                throw new ServiceUnavailableException("PAYMENT_SERVICE_UNAVAILABLE", "Zahlungsservice derzeit nicht erreichbar.");

            }

            CheckoutResponse checkoutResponse = orderTransactionHelper.updateToAwaitingPayment(order.getId(), payment);
            log.info(
                    "Checkout erfolgreich gestartet",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", order.getId(),
                            "paymentStatus", payment.status(),
                            "reservationId", reservation.reservationId(),
                            "durationMs", System.currentTimeMillis() - start
                    ))
            );
            return checkoutResponse;

        } catch (ServiceUnavailableException ex) {

            log.warn(
                    "Externer Service nicht verfügbar",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", order.getId(),
                            "status", order.getStatus(),
                            "errorCode", ex.getErrorCode(),
                            "errorMessage", ex.getMessage()
                    ))
            );

            throw ex;
        } catch (BusinessException ex) {

            log.warn(
                    "Business Fehler im Order-Prozess",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", order.getId(),
                            "status", order.getStatus(),
                            "errorCode", ex.getErrorCode(),
                            "errorMessage", ex.getMessage()
                    ))
            );

            throw ex;
        } catch (PaymentServiceException ex) {

            log.warn(
                    "Payment Fehler im Order-Prozess",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", order.getId(),
                            "status", order.getStatus(),
                            "errorMessage", ex.getMessage()
                    ))
            );

            throw ex;
        } catch (Exception ex) {

            log.error(
                    "Unerwarteter Fehler im Order-Prozess",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "orderId", order.getId(),
                            "status", order.getStatus(),
                            "errorMessage", ex.getMessage()
                    ))
            );


            UUID resId =
                    (reservation != null)
                            ? reservation.reservationId()
                            : null;


            toolService.cancelOrder(
                    order,
                    resId,
                    OrderStatus.CANCELLED,
                    "ORDER_PROCESSING_INTERNAL_ERROR",
                    ex
            );


            throw new GlobalException(
                    "INTERNAL_SYSTEM_ERROR",
                    "Ein interner Fehler ist aufgetreten. Bestellung storniert."
            );
        }

    }


    public OrderDetailsResponse getOrder(UUID orderId) {

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return toolService.toResponse(order);
    }

    public Map<String, String> capturePaypal(String token) {
        return paymentClient.capturePaypal(token);
    }


}