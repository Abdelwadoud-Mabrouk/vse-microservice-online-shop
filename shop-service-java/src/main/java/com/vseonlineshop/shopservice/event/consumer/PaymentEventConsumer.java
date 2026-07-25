package com.vseonlineshop.shopservice.event.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.domain.OrderStatus;
import com.vseonlineshop.shopservice.dto.payment.RefundResponseDto;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationCommitedResponse;
import com.vseonlineshop.shopservice.dto.warehouse.ReservationResponse;
import com.vseonlineshop.shopservice.event.dtos.DomainEvent;
import com.vseonlineshop.shopservice.event.dtos.payment.PaymentFailedPayload;
import com.vseonlineshop.shopservice.event.dtos.payment.PaymentSucceededPayload;
import com.vseonlineshop.shopservice.event.dtos.system.SystemAlertPayload;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import com.vseonlineshop.shopservice.event.producer.KafkaEventPublisher;
import com.vseonlineshop.shopservice.service.client.BillingService;
import com.vseonlineshop.shopservice.service.client.WarehouseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WarehouseClient warehouseClient;
    private final BillingService paymentClient;


    /**
     * Hört auf das Payment-Topic und fängt die asynchrone Erfolgsmeldung ab.
     */

    @KafkaListener(
            topics = "${kafka.topics.payment}",
            groupId = "shop-order-service-group"
    )
    @Transactional
    public void onPaymentEvent(ConsumerRecord<String, DomainEvent<Object>> record) {
        DomainEvent<?> domainEvent = record.value();

        log.info("[Kafka-Consumer] Event empfangen. Typ: {}, Source: {}, CorrelationId: {}",
                domainEvent.eventType(), domainEvent.service(), domainEvent.correlationId());

        if ("PAYMENT_SUCCEEDED".equalsIgnoreCase(domainEvent.eventType())) {
            handlePaymentSucceeded(domainEvent);
        }
        if ("PAYMENT_FAILED".equalsIgnoreCase(domainEvent.eventType())) {
            handlePaymentFailed(domainEvent);
        }
    }


    private void handlePaymentSucceeded(DomainEvent<?> domainEvent) {
        Order order = null;
        try {
            PaymentSucceededPayload payload = objectMapper.convertValue(
                    domainEvent.payload(),
                    PaymentSucceededPayload.class
            );

            log.info("[Kafka-Consumer] Processing PAYMENT_SUCCEEDED for OrderId={}", payload.orderId());

            order = orderRepository.findById(payload.orderId())
                    .orElseThrow(() -> new IllegalStateException("Order not found for ID: " + payload.orderId()));

            // Idempotency check
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.ROLLBACK_COMPLETED) {
                log.warn("[Kafka-Consumer] Order {} already final ({}). Event ignored.", order.getId(), order.getStatus());
                return;
            }

            order.setPaymentId(payload.paymentId().toString());
            orderRepository.save(order);

            // STEP 2: Warehouse reservieren
            ReservationCommitedResponse commitResponse = null;
            try {
                commitResponse = warehouseClient.reservationCommited(order);
            } catch (RestClientException ex) {
                log.error("[WAREHOUSE ERROR] Warehouse unreachable for Order {}", order.getId(), ex);
                handleDispatchFailed(order, "WAREHOUSE_UNREACHABLE");
                return;
            }

            // Safety check: was dispatch really successful
            if (commitResponse == null
                    || !commitResponse.orderId().equals(order.getId())
                    || !commitResponse.correlationId().equals(order.getCorrelationId())
                    || !commitResponse.status().equalsIgnoreCase("COMMITTED")) {

                log.error("[WAREHOUSE] Unexpected dispatch response for Order {}", order.getId());
                handleDispatchFailed(order, "DESPATCH_FAILED");
                return;
            }


            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            log.info("[Kafka-Consumer] Order {} successfully updated to ORDER_COMPLETED.", order.getId());

            Order finalOrder = order;
            safePublish(() -> kafkaEventPublisher.publishOrderCompleted(finalOrder));

        } catch (Exception ex) {
            log.error("[Kafka-Consumer ERROR] Error processing payment event: {}", ex.getMessage(), ex);

            Map<String, String> metadata = Map.of(
                    "correlationId", order != null ? order.getCorrelationId().toString() : "Unknown",
                    "exceptionClass", ex.getClass().getSimpleName()
            );

            SystemAlertPayload alertPayload = new SystemAlertPayload(
                    UUID.randomUUID().toString(),
                    "shop-service",
                    order != null ? order.getId().toString() : "N/A",
                    "Critical failure! Reason: " + ex.getMessage(),
                    Instant.now(),
                    metadata
            );

            Order finalOrder1 = order;
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(finalOrder1, alertPayload));
        }
    }

    private void handleDispatchFailed(Order order, String reason) {

        safePublish(() -> kafkaEventPublisher.publishWarehouseDispatchFailed(order, reason));
        try {
            RefundResponseDto refundResponse = paymentClient.refund(order);

            order.setStatus(OrderStatus.ROLLBACK_COMPLETED);
            orderRepository.save(order);

            log.info("[Kafka-Consumer] Order {} set to ROLLBACK_COMPLETED, refund successful: {}",
                    order.getId(), refundResponse.providerRefundTransactionId());

            safePublish(() -> kafkaEventPublisher.publishOrderRollbackCompleted(order, refundResponse,reason));

        } catch (Exception refundEx) {
            log.error("[BILLING ERROR] Refund failed for Order {}: {}", order.getId(), refundEx.getMessage());

            order.setStatus(OrderStatus.REFUND_PENDING);
            orderRepository.save(order);


            systemAlert(order, "REFUND_FAILED_MANUAL_INTERVENTION_NEEDED", refundEx);
        }
    }

    private void handlePaymentFailed(DomainEvent<?> domainEvent) {
        Order order = null;
        try {
            PaymentFailedPayload payload = objectMapper.convertValue(
                    domainEvent.payload(),
                    PaymentFailedPayload.class
            );

            log.info("[Kafka-Consumer] Verarbeitung PAYMENT_FAILED für OrderId={}", payload.orderId());

            order = orderRepository.findById(payload.orderId())
                    .orElseThrow(() -> new IllegalStateException("Order nicht gefunden für ID: " + payload.orderId()));

            if (order.getStatus() == OrderStatus.PAYMENT_FAILED
                    || order.getStatus() == OrderStatus.COMPLETED
                    || order.getStatus() == OrderStatus.ROLLBACK_COMPLETED) {
                log.warn("[Kafka-Consumer] Order {} bereits final ({}). Event wird ignoriert.", order.getId(), order.getStatus());
                return;
            }

            order.setStatus(OrderStatus.PAYMENT_FAILED);
            order.setPaymentId(payload.paymentId().toString());
            orderRepository.save(order);

            log.info("[Kafka-Consumer] Order {} erfolgreich auf PAYMENT_FAILED aktualisiert.", order.getId());

            Order finalOrder = order;
            safePublish(() -> kafkaEventPublisher.publishOrderCancelled(finalOrder, payload.failureReason()));

            // Reales Reservations

            log.info(
                    "[OrderService] Triggering warehouse reservation rollback. reservationId={}, orderId={}",
                    order.getReservationId(),
                    order.getId());
            safePublish(() -> kafkaEventPublisher.publishReservationRollbackRequested(finalOrder));

            try {
                ReservationResponse response = warehouseClient.cancelReservation(order);

                log.info(
                        "[WarehouseClient] Reservation cancelled successfully. reservationId={}, orderId={}, status={}",
                        response.reservationId(),
                        response.orderId(),
                        response.status());
            } catch (RestClientException ex) {
                log.error("[WAREHOUSE ERROR] Could not release reservation for Order {}", order.getId(), ex);
                systemAlert(order, "RESERVATION_RELEASE_FAILED", ex);
            }

        } catch (Exception ex) {
            log.error("[Kafka-Consumer ERROR] Fehler beim Verarbeiten des Zahlungs-Events: {}", ex.getMessage(), ex);
            systemAlert(order, "INTERNAL_ERROR", ex);
        }
    }

    private void systemAlert(Order order, String reason, Throwable exception) {

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
            log.error("[Kafka-Consumer] Konnte Kafka-Event nicht absenden: {}", e.getMessage());
        }
    }

}