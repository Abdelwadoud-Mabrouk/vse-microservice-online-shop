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

import static net.logstash.logback.argument.StructuredArguments.kv;

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

        log.info(
                "Kafka Event empfangen",
                kv("correlationId", domainEvent.correlationId()),
                kv("service", "shop-service"),
                kv("context", Map.of(
                        "eventType", domainEvent.eventType(),
                        "sourceService", domainEvent.service()
                ))
        );


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

            log.info(
                    "PAYMENT_SUCCEEDED Event wird verarbeitet",
                    kv("correlationId", domainEvent.correlationId()),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "orderId", payload.orderId(),
                            "eventType", domainEvent.eventType()
                    ))
            );

            order = orderRepository.findById(payload.orderId())
                    .orElseThrow(() -> new IllegalStateException("Order not found for ID: " + payload.orderId()));

            // Idempotency check
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.ROLLBACK_COMPLETED) {
                log.warn(
                        "Order bereits final - Event ignoriert",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", Map.of(
                                "orderId", order.getId(),
                                "status", order.getStatus()
                        ))
                );
                return;
            }

            order.setPaymentId(payload.paymentId().toString());
            orderRepository.save(order);

            // STEP 2: Warehouse reservieren
            ReservationCommitedResponse commitResponse = null;
            try {
                commitResponse = warehouseClient.reservationCommited(order);
            } catch (RestClientException ex) {

                log.error(
                        "Warehouse Commit fehlgeschlagen - starte Refund Compensation",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", Map.of(
                                "orderId", order.getId(),
                                "errorMessage", ex.getMessage()
                        )),
                        ex
                );

                handleDispatchFailed(order, "WAREHOUSE_COMMIT_FAILED");
                return;
            }

            // Safety check: was dispatch really successful
            if (commitResponse == null
                    || !commitResponse.orderId().equals(order.getId())
                    || !commitResponse.correlationId().equals(order.getCorrelationId())
                    || !commitResponse.status().equalsIgnoreCase("COMMITTED")) {

                log.error(
                        "Unerwartete Warehouse Commit Antwort",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", Map.of(
                                "orderId", order.getId(),
                                "commitResponse", String.valueOf(commitResponse)
                        ))
                );
                handleDispatchFailed(order, "DESPATCH_FAILED");
                return;
            }


            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            log.info(
                    "Order erfolgreich abgeschlossen",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "orderId", order.getId(),
                            "status", OrderStatus.COMPLETED
                    ))
            );

            Order finalOrder = order;
            safePublish(() -> kafkaEventPublisher.publishOrderCompleted(finalOrder));

        } catch (Exception ex) {
            log.error(
                    "Fehler beim Verarbeiten von PAYMENT_SUCCEEDED",
                    kv(
                            "correlationId",
                            order != null ? order.getCorrelationId() : domainEvent.correlationId()
                    ),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "orderId", order != null ? order.getId() : "N/A",
                            "errorMessage", ex.getMessage()
                    )),
                    ex
            );

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
        log.warn(
                "Warehouse Dispatch fehlgeschlagen - starte Compensation",
                kv("correlationId", order.getCorrelationId()),
                kv("service", "shop-service"),
                kv("context", Map.of(
                        "orderId", order.getId(),
                        "reason", reason
                ))
        );


        safePublish(() -> kafkaEventPublisher.publishWarehouseDispatchFailed(order, reason));
        try {
            RefundResponseDto refundResponse = paymentClient.refund(order);

            order.setStatus(OrderStatus.ROLLBACK_COMPLETED);
            orderRepository.save(order);

            log.info(
                    "Refund erfolgreich - Order Rollback abgeschlossen",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "orderId", order.getId(),
                            "status", order.getStatus(),
                            "refundTransactionId", refundResponse.providerRefundTransactionId(),
                            "reason", reason
                    ))
            );

            safePublish(() -> kafkaEventPublisher.publishOrderRollbackCompleted(order, refundResponse, reason));

        } catch (Exception refundEx) {
            log.error(
                    "Refund beim Billing-Service fehlgeschlagen",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "orderId", order.getId(),
                            "reason", reason,
                            "errorMessage", refundEx.getMessage()
                    )),
                    refundEx
            );

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

            log.info(
                    "PAYMENT_FAILED Event wird verarbeitet",
                    kv("correlationId", domainEvent.correlationId()),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "eventType", domainEvent.eventType(),
                            "orderId", payload.orderId()
                    ))
            );


            order = orderRepository.findById(payload.orderId())
                    .orElseThrow(() -> new IllegalStateException("Order nicht gefunden für ID: " + payload.orderId()));

            if (order.getStatus() == OrderStatus.PAYMENT_FAILED
                    || order.getStatus() == OrderStatus.COMPLETED
                    || order.getStatus() == OrderStatus.ROLLBACK_COMPLETED) {
                log.warn(
                        "PAYMENT_FAILED Event ignoriert - Order bereits final",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", Map.of(
                                "orderId", order.getId(),
                                "currentStatus", order.getStatus()
                        ))
                );

                return;
            }

            order.setStatus(OrderStatus.PAYMENT_FAILED);
            order.setPaymentId(payload.paymentId().toString());
            orderRepository.save(order);

            log.info(
                    "Order Status auf PAYMENT_FAILED gesetzt",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "orderId", order.getId(),
                            "status", order.getStatus()
                    ))
            );

            Order finalOrder = order;
            safePublish(() -> kafkaEventPublisher.publishOrderCancelled(finalOrder, payload.failureReason()));


            log.info(
                    "Warehouse Reservation Rollback gestartet",
                    kv("correlationId", order.getCorrelationId()),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "orderId", order.getId(),
                            "reservationId",
                            order.getReservationId() != null
                                    ? order.getReservationId()
                                    : "N/A"
                    ))
            );

            safePublish(() -> kafkaEventPublisher.publishReservationRollbackRequested(finalOrder));

            ReservationResponse response = null;
            try {
                response = warehouseClient.cancelReservation(order);

                log.info(
                        "[WarehouseClient] Reservation cancelled successfully. reservationId={}, orderId={}, status={}",
                        response.reservationId(),
                        response.orderId(),
                        response.status());
            } catch (RestClientException ex) {

                assert response != null;
                log.info(
                        "Warehouse Reservation erfolgreich freigegeben",
                        kv("correlationId", order.getCorrelationId()),
                        kv("service", "shop-service"),
                        kv("context", Map.of(
                                "orderId", order.getId(),
                                "reservationId", order.getCorrelationId(),
                                "status", response.status()
                        ))
                );
                systemAlert(order, "RESERVATION_RELEASE_FAILED", ex);
            }

        } catch (Exception ex) {
            log.error(
                    "Fehler bei Verarbeitung von PAYMENT_FAILED Event",
                    kv(
                            "correlationId",
                            order != null
                                    ? order.getCorrelationId()
                                    : domainEvent.correlationId()
                    ),
                    kv("service", "shop-service"),
                    kv("context", Map.of(
                            "orderId",
                            order != null
                                    ? order.getId()
                                    : "N/A",
                            "errorMessage",
                            ex.getMessage()
                    )),
                    ex
            );
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
            log.error("Kafka Event konnte nicht gesendet werden", kv("service", "shop-service"), kv("context", Map.of("errorMessage", e.getMessage())), e);
        }
    }

}