package com.vseonlineshop.shopservice.service.shop;

import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.domain.OrderItem;
import com.vseonlineshop.shopservice.domain.OrderStatus;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import com.vseonlineshop.shopservice.dto.shop.OrderItemResponse;
import com.vseonlineshop.shopservice.dto.shop.OrderRequest;
import com.vseonlineshop.shopservice.dto.shop.OrderResponse;
import com.vseonlineshop.shopservice.dto.warehouse.*;
import com.vseonlineshop.shopservice.event.dtos.system.SystemAlertPayload;
import com.vseonlineshop.shopservice.exception.*;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import com.vseonlineshop.shopservice.event.producer.KafkaEventPublisher;
import com.vseonlineshop.shopservice.service.client.PaymentClient;
import com.vseonlineshop.shopservice.service.client.WarehouseClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

    private final OrderRepository orderRepository;
    private final WarehouseClient warehouseClient;
    private final PaymentClient paymentClient;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final OrderTransactionHelper orderTransactionHelper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Create Order ─────────────────────────────────────────────────────────
    public OrderResponse createOrder(OrderRequest request) {
        Order order;

        // ── STEP 1: Initiales Anlegen ───────────────────
        try {
            order = orderTransactionHelper.createInitialOrder(request);
            log.info("[OrderService] Order {} angelegt (correlationId={})", order.getId(), order.getCorrelationId());
            Order finalOrder = order;
            safePublish(() -> kafkaEventPublisher.publishOrderCreated(finalOrder));
        } catch (Exception ex) {
            log.error("[CRITICAL] Konnte initiale Order nicht in DB anlegen: {}", ex.getMessage());
            cancelOrder(null, null, "CREATION_ORDER_FAILED", ex);
            throw new GlobalException("DATABASE_ERROR", "Bestellung konnte nicht angelegt werden.");
        }

        ReservationResponse reservation = null;

        try {
            // STEP 2: Warehouse reservieren
            try {
                reservation = warehouseClient.reserve(order);
            } catch (RestClientException ex) {
                log.error("[WAREHOUSE ERROR] Warehouse unreachable for Order {}", order.getId(), ex);
                cancelOrder(order, null, "WAREHOUSE_SERVICE_UNAVAILABLE", ex);
                throw new ServiceUnavailableException("WAREHOUSE_SERVICE_UNAVAILABLE", "Lager offline.");
            }

            if (reservation.items() == null || reservation.items().isEmpty()) {
                cancelOrder(order, null, "EMPTY_RESERVATION", null);
                throw new BusinessException("Warehouse returned empty items.");
            }

            // STEP 3: Total berechnen & Update auf PROCESSING
            BigDecimal total = calculateTotal(order.getItems());

            // Save ReservationId in order Tabelle (needed to commit reservation)
            order = orderTransactionHelper.updateToProcessing(order.getId(), total, reservation.reservationId());

            // STEP 4: Payment aufrufen
            PaymentResponse payment;
            try {
                payment = paymentClient.pay(order);
            } catch (RestClientException ex) {
                log.error("[PAYMENT ERROR] Payment-Service unreachable for Order {}", order.getId(), ex);
                cancelOrder(order, reservation.reservationId(), "PAYMENT_SERVICE_UNAVAILABLE", ex);
                throw new ServiceUnavailableException("PAYMENT_SERVICE_UNAVAILABLE", "Zahlungsservice offline.");
            }

            // Fall 1: Sofortiger Zahlungsfehler vom Provider
            if ("FAILED".equalsIgnoreCase(String.valueOf(payment.status()))) {
                cancelOrder(order, reservation.reservationId(), "PAYMENT_FAILED", null);
                throw new PaymentServiceException("Zahlung fehlgeschlagen.");
            }

            // Fall 2: Erfolg / PENDING -> Update auf AWAITING_PAYMENT
            return orderTransactionHelper.updateToAwaitingPayment(order.getId(), payment);

        } catch (Exception ex) {
            log.error("[FATAL CRASH] Unerwarteter Fehler im Order-Prozess für Order {}", order.getId(), ex);


            if (!(ex instanceof ServiceUnavailableException || ex instanceof BusinessException || ex instanceof PaymentServiceException)) {

                UUID resId = (reservation != null) ? reservation.orderId() : null;

                cancelOrder(order, resId, "ORDER_PROCESSING_INTERNAL_ERROR", ex);
                throw new GlobalException("INTERNAL_SYSTEM_ERROR", "Ein interner Fehler ist aufgetreten. Bestellung storniert.");
            }

            throw ex;
        }
    }

    // ── Get Order ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return toResponse(order);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private OrderResponse toResponse(Order order) {
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

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getPaymentId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPaymentMethod(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                null
        );
    }

    private void cancelOrder(Order order, UUID reservationOrderId, String reason, Throwable exception) {

        if (order != null) {
            try {
                order.setStatus(OrderStatus.CANCELLED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("[OrderService] Order {} in DB erfolgreich storniert.", order.getId());
            } catch (Exception dbEx) {
                log.error("[CRITICAL DB ERROR] Konnte Status in DB nicht auf CANCELLED setzen, da die DB blockiert/down ist! Führe Kafka-Rollback trotzdem fort.", dbEx);
            }

            safePublish(() -> kafkaEventPublisher.publishOrderCancelled(order, reason));

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