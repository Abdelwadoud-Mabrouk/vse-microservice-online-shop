package com.vseonlineshop.shopservice.event.producer;


import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.domain.OrderItem;
import com.vseonlineshop.shopservice.dto.payment.RefundResponseDto;
import com.vseonlineshop.shopservice.event.dtos.*;
import com.vseonlineshop.shopservice.event.dtos.order.*;
import com.vseonlineshop.shopservice.event.dtos.system.SystemAlertPayload;
import com.vseonlineshop.shopservice.event.dtos.warehouse.ReservationRollbackPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


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


    @Value("${kafka.topics.alerts}")
    private String alertTopic;


    public void publishOrderCreated(Order order) {

        var payload = new OrderCreatedPayload(
                order.getId(),
                order.getUserId(),
                order.getItems().stream().map(OrderItem::getProductId).toList(),
                order.getCurrency(),
                order.getPaymentMethod().name()
        );
        send(ordersTopic, order, "ORDER_CREATED", payload);
    }

    public void publishOrderCancelled(Order order, String reason) {
        var payload = new OrderCancelledPayload(
                order.getId(),
                order.getUserId(),
                reason
        );
        send(ordersTopic, order, "ORDER_" + order.getStatus(), payload);
    }

    public void publishOrderCompleted(Order order) {

        var customer = new CustomerPayload(
                "Mock User",
                "mock@email.com",
                "Street 1",
                "00000",
                "Berlin",
                "DE"
        );

        var payload = new OrderCompletedPayload(
                order.getId(),
                order.getUserId(),
                customer.email(),
                order.getItems().stream()
                        .map(i -> new InvoiceItem(
                                i.getSku(),
                                i.getProductName(),
                                i.getQuantity(),
                                i.getPricePerUnit()
                        ))
                        .toList(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCorrelationId(),
                order.getPaymentId(),
                customer,
                order.getPaymentMethod().name()
        );
        send(ordersTopic, order, "ORDER_COMPLETED", payload);
    }

    public void publishOrderRollbackCompleted(Order order, RefundResponseDto refundResponse, String reason) {
        var payload = new OrderRollbackCompletedPayload(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCorrelationId(),
                order.getPaymentId(),
                refundResponse.providerRefundTransactionId(),
                reason

        );

        send(ordersTopic, order, "ORDER_ROLLBACK_COMPLETED", payload);
    }

    public void publishWarehouseDispatchFailed(Order order, String reason) {
        var payload = new WarehouseDispatchFailedPayload(
                order.getId(),
                order.getCorrelationId(),
                reason
        );
        send(ordersTopic, order, "WAREHOUSE_DISPATCH_FAILED", payload);
    }

    public void publishReservationRollbackRequested(Order order) {
        var payload = new ReservationRollbackPayload(
                order.getId(),
                order.getReservationId()
        );
        send(ordersTopic, order, "RESERVATION_ROLLBACK_REQUESTED", payload);
    }


    public void publishSystemAlert(Order order, SystemAlertPayload payload) {
        send(alertTopic, order, "SYSTEM_ALERT", payload);
    }

    // ── SYSTEM_ALERT ──────────────────────────────────────────────────────────

    private void send(String topic, Order order, String eventType, Object payload) {

        var event = DomainEvent.of(
                eventType,
                "shop-service",
                order != null ? order.getCorrelationId() : null,
                payload
        );

        log.info("AUDIT TOPIC CONFIG = {}", topic);
        log.info("TARGET TOPIC = {}", topic);
        log.info("EVENT TYPE = {}, ORDER ID = {}", eventType, order != null ? order.getId() : null);
        log.info("EVENT PAYLOAD = {}", event);

        if (order == null) {
            log.error("ORDER IS NULL → abort send()");
            return;
        }

        try {
            var future = kafkaTemplate.send(topic, order.getId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error(" KAFKA SEND FAILED");
                    log.error("   → eventType = {}", eventType);
                    log.error("   → orderId   = {}", order.getId());
                    log.error("   → topic     = {}", topic);
                    log.error("   → error     = {}", ex.getMessage(), ex);
                } else {
                    log.info(" KAFKA SEND SUCCESS");
                    log.info("   → eventType = {}", eventType);
                    log.info("   → orderId   = {}", order.getId());
                    log.info("   → topic     = {}", result.getRecordMetadata().topic());
                    log.info("   → partition = {}", result.getRecordMetadata().partition());
                    log.info("   → offset    = {}", result.getRecordMetadata().offset());
                }
            });

            future.get();

        } catch (Exception e) {
            log.error(" EXCEPTION DURING KAFKA SEND", e);
        }
    }


}