package com.vseonlineshop.shopservice.service.shop;

import com.vseonlineshop.shopservice.domain.*;
import com.vseonlineshop.shopservice.dto.order.response.*;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.dto.order.request.OrderRequest;
import com.vseonlineshop.shopservice.dto.warehouse.*;
import com.vseonlineshop.shopservice.event.dtos.system.SystemAlertPayload;
import com.vseonlineshop.shopservice.exception.*;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import com.vseonlineshop.shopservice.event.producer.KafkaEventPublisher;
import com.vseonlineshop.shopservice.service.client.BillingService;
import com.vseonlineshop.shopservice.service.client.WarehouseClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

    private final OrderRepository orderRepository;
    private final WarehouseClient warehouseClient;
    private final BillingService paymentClient;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final OrderTransactionHelper orderTransactionHelper;

    // ── Create Order ─────────────────────────────────────────────────────────
    public CheckoutResponse createOrder(OrderRequest request) {
        Order order;

        // ── STEP 1: Initiales Anlegen ───────────────────
        try {
            order = orderTransactionHelper.createInitialOrder(request);
            log.info("[OrderService] Order {} angelegt (correlationId={})", order.getId(), order.getCorrelationId());
            Order finalOrder = order;
            safePublish(() -> kafkaEventPublisher.publishOrderCreated(finalOrder));
        } catch (Exception ex) {
            log.error("[CRITICAL] Konnte initiale Order nicht in DB anlegen: {}", ex.getMessage());
            cancelOrder(null, null, OrderStatus.CANCELLED,"CREATION_ORDER_FAILED", ex);
            throw new GlobalException("DATABASE_ERROR", "Bestellung konnte nicht angelegt werden.");
        }

        ReservationResponse reservation = null;

        try {
            // STEP 2: Warehouse reservieren
            try {
                reservation = warehouseClient.reserve(order);
            } catch (HttpClientErrorException ex) {
                // 4xx Fehler (z.B. 409 Conflict / 400 Bad Request): Lager da, aber Bestand reicht nicht
                log.warn("[WAREHOUSE OUT_OF_STOCK] Bestand reicht nicht für Order {}: {}", order.getId(), ex.getMessage());

                // Setzt Status auf OUT_OF_STOCK und bricht SOFORT ab (kein Retry/weiterer Aufruf)
                cancelOrder(order, null, OrderStatus.OUT_OF_STOCK,"OUT_OF_STOCK", ex);
                throw new BusinessException("OUT_OF_STOCK", HttpStatus.CONFLICT);

            } catch (ResourceAccessException | HttpServerErrorException ex) {
                // 5xx Server Error oder Timeout/Connection Refused: Service wirklich DOWN
                log.error("[WAREHOUSE OFFLINE] Lager-Service nicht erreichbar für Order {}", order.getId(), ex);

                cancelOrder(order, null, OrderStatus.CANCELLED,"WAREHOUSE_SERVICE_UNAVAILABLE", ex);
                throw new ServiceUnavailableException("WAREHOUSE_SERVICE_UNAVAILABLE", "Lager-Service derzeit nicht erreichbar.");
            }

            // Prüfung auf leere Antwort
            if (reservation == null || reservation.items() == null || reservation.items().isEmpty()) {
                cancelOrder(order, null, OrderStatus.CANCELLED,"EMPTY_RESERVATION", null);
                throw new BusinessException("EMPTY_RESERVATION",HttpStatus.BAD_REQUEST);
            }


            // STEP 3: Total berechnen & Update auf PROCESSING
            BigDecimal total = calculateTotal(order.getItems());

            // Save ReservationId in order Tabelle (needed to commit reservation)
            order = orderTransactionHelper.updateToProcessing(order.getId(), total, reservation.reservationId());

            // STEP 4: Payment aufrufen
            PaymentResponse payment;
            try {
                payment = paymentClient.pay(order);
            } catch (HttpClientErrorException ex) {
                // 4xx: Payment-Service DA, hat aber Request abgelehnt (z.B. Bad Request 400)
                log.warn("[PAYMENT CLIENT ERROR] Fehlerhafte Zahlungsanfrage für Order {}: HTTP {}", order.getId(), ex.getStatusCode());

                // Rollback durchführen (Lager freigeben)
                cancelOrder(order, reservation.reservationId(), OrderStatus.PAYMENT_FAILED, "INVALID_PAYMENT_REQUEST", ex);

                throw new BusinessException("INVALID_PAYMENT_REQUEST", HttpStatus.PAYMENT_REQUIRED);

            } catch (ResourceAccessException | HttpServerErrorException ex) {
                // 5xx / Timeout: Nach 4 Retries immer noch offline/down
                log.error("[PAYMENT OFFLINE] Payment-Service nach Retries nicht erreichbar für Order {}", order.getId(), ex);

                // Rollback durchführen (Lager freigeben)
                cancelOrder(order, reservation.reservationId(), OrderStatus.PAYMENT_FAILED, "PAYMENT_SERVICE_UNAVAILABLE", ex);

                throw new ServiceUnavailableException("PAYMENT_SERVICE_UNAVAILABLE", "Zahlungsservice derzeit nicht erreichbar.");
            }

            // Fall 1: Sofortiger Zahlungsfehler vom Provider
            if ("FAILED".equalsIgnoreCase(String.valueOf(payment.status()))) {
                cancelOrder(order, reservation.reservationId(), OrderStatus.CANCELLED,"PAYMENT_FAILED", null);
                throw new PaymentServiceException("Zahlung fehlgeschlagen.");
            }

            // Fall 2: Erfolg / PENDING -> Update auf AWAITING_PAYMENT
            return orderTransactionHelper.updateToAwaitingPayment(order.getId(), payment);

        } catch (Exception ex) {
            log.error("[FATAL CRASH] Unerwarteter Fehler im Order-Prozess für Order {}", order.getId(), ex);


            if (!(ex instanceof ServiceUnavailableException || ex instanceof BusinessException || ex instanceof PaymentServiceException)) {

                UUID resId = (reservation != null) ? reservation.orderId() : null;

                cancelOrder(order, resId, OrderStatus.CANCELLED,"ORDER_PROCESSING_INTERNAL_ERROR", ex);
                throw new GlobalException("INTERNAL_SYSTEM_ERROR", "Ein interner Fehler ist aufgetreten. Bestellung storniert.");
            }

            throw ex;
        }
    }


    public OrderDetailsResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return toResponse(order);
    }

    public Map<String, String> capturePaypal(String token) {
        return paymentClient.capturePaypal(token);
    }
    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private OrderDetailsResponse toResponse(Order order) {

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPricePerUnit(),
                        item.getSubTotal(),
                        item.getTaxRate(),
                        item.getTaxAmount(),
                        item.getTotalPrice()
                ))
                .toList();


        CustomerInformation customer = order.getCustomer();

        CustomerInformationResponse customerResponse =
                new CustomerInformationResponse(
                        customer.getFirstName(),
                        customer.getLastName(),
                        customer.getEmail(),
                        customer.getPhone()
                );


        ShippingAddress shippingAddress = order.getShippingAddress();

        ShippingAddressResponse shippingAddressResponse =
                new ShippingAddressResponse(
                        shippingAddress.getStreet(),
                        shippingAddress.getHouseNumber(),
                        shippingAddress.getPostalCode(),
                        shippingAddress.getCity(),
                        shippingAddress.getCountry()
                );


        return new OrderDetailsResponse(
                order.getId(),
                customerResponse,
                shippingAddressResponse,
                itemResponses,
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }


    private void cancelOrder(Order order, UUID reservationOrderId, OrderStatus targetStatus, String reason, Throwable exception) {

        if (order != null) {
            try {
                // Verwendet direkt den übergebenen Target-Status (z.B. OUT_OF_STOCK oder CANCELLED)
                order.setStatus(targetStatus);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("[OrderService] Order {} in DB erfolgreich auf Status {} gesetzt.", order.getId(), targetStatus);
            } catch (Exception dbEx) {
                log.error("[CRITICAL DB ERROR] Konnte Status in DB nicht auf {} setzen! Führe Rollback trotzdem fort.", targetStatus, dbEx);
            }

            // Event über Kafka publizieren
            safePublish(() -> kafkaEventPublisher.publishOrderCancelled(order, reason));

            // Lager-Reservierung stornieren (nur falls bereits eine Reservierung stattfand)
            if (reservationOrderId != null) {
                log.info(
                        "[OrderService] Triggering warehouse reservation rollback. reservationId={}, orderId={}",
                        reservationOrderId,
                        order.getId());

                safePublish(() -> kafkaEventPublisher.publishReservationRollbackRequested(order));

                try {
                    ReservationResponse response = warehouseClient.cancelReservation(order);

                    log.info(
                            "[WarehouseClient] Reservation cancelled successfully. reservationId={}, orderId={}, status={}",
                            response.reservationId(),
                            response.orderId(),
                            response.status());

                } catch (Exception ex) {
                    log.error(
                            "[WarehouseClient] Failed to cancel reservation. reservationId={}, orderId={}, correlationId={}",
                            reservationOrderId,
                            order.getId(),
                            order.getCorrelationId(),
                            ex);
                }
            }
        }

        // System-Alert NUR bei echten technischen Ausfällen/Stornierungen senden (nicht bei Fachlogik wie OUT_OF_STOCK)
        if (targetStatus != OrderStatus.OUT_OF_STOCK) {
            Map<String, String> metadata = Map.of(
                    "correlationId", order != null ? order.getCorrelationId().toString() : "Unknown",
                    "exceptionClass", exception != null ? exception.getClass().getSimpleName() : "Unknown"
            );

            SystemAlertPayload alertPayload = new SystemAlertPayload(
                    UUID.randomUUID().toString(),
                    "shop-service",
                    order != null ? order.getId().toString() : "N/A",
                    "Kritischer Ausfall! Grund: " + reason + " | Details: " + (exception != null ? exception.getMessage() : "Keine"),
                    Instant.now(),
                    metadata
            );

            safePublish(() -> kafkaEventPublisher.publishSystemAlert(order, alertPayload));
        } else {
            log.info("[OrderService] Kein SystemAlert gesendet (Reguläre Fachlogik: OUT_OF_STOCK). Order={}",
                    order != null ? order.getId() : "N/A");
        }
    }

    private void safePublish(Runnable publishAction) {
        try {
            publishAction.run();
        } catch (Exception e) {
            // todo: Wie können wir damit umgehen, damit wir die Nachvollziehbarkeit nicht verlieren können : Transactional Outbox Pattern.
            log.error("[Kafka] Event konnte nicht gesendet werden: {}", e.getMessage(), e);
        }
    }

}