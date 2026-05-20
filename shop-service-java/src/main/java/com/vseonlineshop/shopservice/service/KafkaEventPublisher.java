package com.vseonlineshop.shopservice.service;


import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.domain.OrderItem;
import com.vseonlineshop.shopservice.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Fire-and-forget Kafka Publisher.
 * Fehler werden geloggt aber NICHT propagiert – der Geschäftsfluss
 * darf durch einen Kafka-Ausfall nicht blockiert werden.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.orders}")
    private String ordersTopic;


    public void publishOrderCreated(Order order) {

        var payload = new OrderCreatedPayload(
                order.getId(),
                order.getUserId(),
                order.getItems().stream().map(OrderItem::getProductId).toList(),
                order.getCurrency(),
                order.getPaymentProvider().name()
        );
        send(ordersTopic, order, "ORDER_CREATED", payload);
    }

    public void publishOrderCancelled(Order order, String reason) {
        var payload = new OrderCancelledPayload(
                order.getId(),
                order.getUserId(),
                reason
        );
        send(ordersTopic, order, "ORDER_CANCELLED", payload);
    }

    public void publishOrderCompleted(Order order) {
        var payload = new OrderCompletedPayload(
                order.getId(),
                order.getUserId(),
                order.getPaymentId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPaymentProvider().name()
        );
        send(ordersTopic, order, "ORDER_COMPLETED", payload);
    }

    public void publishReservationRollbackRequested(Order order, UUID reservationId) {
        var payload = new ReservationRollbackPayload(
                order.getId(),
                reservationId
        );
        send(ordersTopic, order, "RESERVATION_ROLLBACK_REQUESTED", payload);
    }


    private void send(String topic, Order order, String eventType, Object payload) {
        var event = DomainEvent.of(eventType, "shop-service", order.getCorrelationId(), payload);

        log.info("Sending event {} to topic {}", event, topic);

        kafkaTemplate.send(topic, order.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] FEHLER → eventType='{}' orderId='{}': {}",
                                eventType, order.getId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Gesendet → eventType='{}' topic='{}' partition={} offset={}",
                                eventType, topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}