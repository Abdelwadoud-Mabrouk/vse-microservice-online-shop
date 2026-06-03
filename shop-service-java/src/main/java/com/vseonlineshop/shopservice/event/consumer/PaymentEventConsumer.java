package com.vseonlineshop.shopservice.event.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.domain.OrderStatus;
import com.vseonlineshop.shopservice.event.dtos.DomainEvent;
import com.vseonlineshop.shopservice.event.dtos.payment.PaymentFailedPayload;
import com.vseonlineshop.shopservice.event.dtos.payment.PaymentSucceededPayload;
import com.vseonlineshop.shopservice.event.dtos.system.SystemAlertPayload;
import com.vseonlineshop.shopservice.repository.OrderRepository;
import com.vseonlineshop.shopservice.event.producer.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Hört auf das Payment-Topic und fängt die asynchrone Erfolgsmeldung ab.
     */

    @KafkaListener(
            topics = "${kafka.topics.audit-events}",
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

            log.info("[Kafka-Consumer] Verarbeitung PAYMENT_SUCCEEDED für OrderId={}", payload.orderId());

            order = orderRepository.findById(payload.orderId())
                    .orElseThrow(() -> new IllegalStateException("Order nicht gefunden für ID: " + payload.orderId()));

            if (order.getStatus() == OrderStatus.COMPLETED) {
                log.warn("[Kafka-Consumer] Order {} ist bereits COMPLETED. Event wird ignoriert.", order.getId());
                return;
            }

            order.setStatus(OrderStatus.COMPLETED);
            order.setPaymentId(payload.paymentId().toString());
            orderRepository.save(order);

            log.info("[Kafka-Consumer] Order {} erfolgreich auf COMPLETED aktualisiert.", order.getId());

            Order finalOrder = order;
            safePublish(() -> kafkaEventPublisher.publishOrderCompleted(finalOrder));
            safePublish(() -> kafkaEventPublisher.publishInvoiceRequested(finalOrder));

        } catch (Exception ex) {
            log.error("[Kafka-Consumer ERROR] Fehler beim Verarbeiten des Zahlungs-Events: {}", ex.getMessage(), ex);

            Map<String, String> metadata = Map.of(
                    "correlationId", order != null ? order.getCorrelationId().toString() : "Unknown",
                    "exceptionClass", ex.getClass().getSimpleName()
            );

            SystemAlertPayload alertPayload = new SystemAlertPayload(
                    UUID.randomUUID().toString(),
                    "shop-service",
                    order != null ? order.getId().toString() : "N/A",
                    "Kritischer Ausfall! Grund: " + ex.getMessage(),
                    Instant.now(),
                    metadata
            );

            Order finalOrder1 = order;
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(finalOrder1, alertPayload));
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

            if (order.getStatus() == OrderStatus.CANCELLED) {
                log.warn("[Kafka-Consumer] Order {} ist bereits CANCELED. Event wird ignoriert.", order.getId());
                return;
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentId(payload.paymentId().toString());
            orderRepository.save(order);

            log.info("[Kafka-Consumer] Order {} erfolgreich auf CANCELED aktualisiert.", order.getId());

            Order finalOrder = order;
            safePublish(() -> kafkaEventPublisher.publishOrderCancelled(finalOrder, payload.failureReason()));

        } catch (Exception ex) {
            log.error("[Kafka-Consumer ERROR] Fehler beim Verarbeiten des Zahlungs-Events: {}", ex.getMessage(), ex);

            Map<String, String> metadata = Map.of(
                    "correlationId", order != null ? order.getCorrelationId().toString() : "Unknown",
                    "exceptionClass", ex.getClass().getSimpleName()
            );

            SystemAlertPayload alertPayload = new SystemAlertPayload(
                    UUID.randomUUID().toString(),
                    "shop-service",
                    order != null ? order.getId().toString() : "N/A",
                    "Kritischer Ausfall! Grund: " + ex.getMessage(),
                    Instant.now(),
                    metadata
            );

            Order finalOrder1 = order;
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(finalOrder1, alertPayload));
        }
    }

    private void safePublish(Runnable publishAction) {
        try {
            publishAction.run();
        } catch (Exception e) {
            log.error("[Kafka-Consumer] Konnte folgendes OrderCompleted-Event nicht absenden: {}", e.getMessage());
        }
    }
}