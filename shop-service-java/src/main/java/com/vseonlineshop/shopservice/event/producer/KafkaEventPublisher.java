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


import static com.vseonlineshop.shopservice.config.LogUtils.safeContext;
import static net.logstash.logback.argument.StructuredArguments.kv;


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
                order.getCustomer().getLastName() + " " + order.getCustomer().getFirstName(),
                order.getCustomer().getEmail(),
                order.getShippingAddress().getStreet(),
                order.getShippingAddress().getPostalCode(),
                order.getShippingAddress().getCity(),
                order.getShippingAddress().getCountry()
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


    private void send(String topic, Order order, String eventType, Object payload) {

        if (order == null) {

            log.error(
                    "Kafka Event konnte nicht gesendet werden - Order ist null",
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "eventType", eventType,
                            "topic", topic
                    ))
            );

            return;
        }

        var event = DomainEvent.of(
                eventType,
                "shop-service",
                order.getCorrelationId(),
                payload
        );

        try {
            var result = kafkaTemplate.send(topic, order.getId().toString(), event).get();

            log.debug(
                    "Kafka Event erfolgreich gesendet",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "eventType", eventType,
                            "orderId", order.getId(),
                            "topic", topic,
                            "partition", result.getRecordMetadata().partition(),
                            "offset", result.getRecordMetadata().offset()
                    ))
            );

        } catch (Exception e) {
            log.error(
                    "Kafka Event Senden fehlgeschlagen",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", safeContext(
                            "eventType", eventType,
                            "orderId", order.getId(),
                            "topic", topic,
                            "errorMessage", e.getMessage()
                    )),
                    e
            );
        }
    }


}