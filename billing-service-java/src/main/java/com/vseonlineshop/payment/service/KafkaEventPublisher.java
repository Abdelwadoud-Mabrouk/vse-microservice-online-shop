package com.vseonlineshop.payment.service;


import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentTransaction;
import com.vseonlineshop.payment.event.*;
import com.vseonlineshop.payment.event.payment.*;
import com.vseonlineshop.payment.event.system.SecurityAlertPayload;
import com.vseonlineshop.payment.event.system.SystemAlertPayload;
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

    @Value("${kafka.topics.payment}")
    private String paymentTopic;
    @Value("${kafka.topics.alerts}")
    private String alertTopic;


    private static final UUID SYSTEM_FALLBACK_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // ── SYSTEM_ALERT ──────────────────────────────────────────────────────────
    public void publishSystemAlert(String providerTxnId, String message) {
        var payload = new SystemAlertPayload(
                UUID.randomUUID().toString(),
                message,
                providerTxnId
        );
        sendAlert(alertTopic, "SYSTEM_ALERT", SYSTEM_FALLBACK_UUID, providerTxnId, payload);
    }

    // ── SECURITY_ALERT ────────────────────────────────────────────────────────
    public void publishSecurityAlert(String invalidPaymentIdRaw, String message) {
        var payload = new SecurityAlertPayload(
                UUID.randomUUID().toString(),
                message,
                invalidPaymentIdRaw
        );

        sendAlert(alertTopic, "SECURITY_ALERT", SYSTEM_FALLBACK_UUID, invalidPaymentIdRaw, payload);
    }

    // ── WEBHOOK_DUPLICATE_IGNORED (Idempotenz-Hit) ─────────────────────────────
    public void publishWebhookDuplicateIgnored(Payment payment) {
        var payload = new WebhookDuplicatePayload(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getCorrelationId(),
                payment.getPaymentMethod(),
                payment.getStatus()
        );
        send(payment, paymentTopic, "WEBHOOK_DUPLICATE_IGNORED", payload);
    }

    // ── PAYMENT_NOTIFICATION_RECEIVED ─────────────────────────────────────────
    public void publishPaymentNotificationReceived(Payment payment, PaymentTransaction txn, String eventType) {
        var payload = new PaymentNotificationPayload(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getCorrelationId(),
                payment.getPaymentMethod(),
                eventType,
                txn.getProviderTransactionId(),
                txn.getRawResponse()
        );

        send(payment, paymentTopic, "PAYMENT_NOTIFICATION_RECEIVED", payload);
    }

    public void publishPaymentCreated(Payment payment) {

        var payload = new PaymentInitialedPayload(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getCorrelationId(),
                payment.getUserId(),
                payment.getPaymentMethod(),
                payment.getCurrency(),
                payment.getStatus()
        );
        send(payment, paymentTopic, "PAYMENT_INITIALIZED", payload);
    }


    // ── PAYMENT_SUCCEEDED ──────────────────────────────────
    public void publishPaymentSucceeded(Payment payment, PaymentTransaction txn) {
        var payload = new PaymentSucceededPayload(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getCorrelationId(),
                payment.getUserId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getCurrency(),
                txn.getProviderTransactionId(),
                txn.getRawResponse()
        );
        send(payment, paymentTopic, "PAYMENT_SUCCEEDED", payload);
    }

    // ── PAYMENT_FAILED ─────────────────────────────────────
    public void publishPaymentFailed(Payment payment, PaymentTransaction txn) {
        var payload = new PaymentFailedPayload(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getCorrelationId(),
                payment.getUserId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getCurrency(),
                txn.getFailureReason(),
                txn.getProviderTransactionId(),
                txn.getRawResponse()
        );
        send(payment, paymentTopic, "PAYMENT_FAILED", payload);
    }

    // ── Interner Send (fire & forget) ──────────────────────
    private void send(Payment payment, String topic, String eventType, Object payload) {
        var event = DomainEvent.of(
                eventType,
                "payment-service",
                payment.getCorrelationId(),
                payload
        );

        log.info("[Kafka] Publishing event={} paymentId={}", eventType, payment.getPaymentId());

        kafkaTemplate.send(topic, payment.getPaymentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] FEHLER → event='{}' paymentId='{}': {}",
                                eventType, payment.getPaymentId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Gesendet → event='{}' topic='{}' partition={} offset={}",
                                eventType, topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    private void sendAlert(String topic, String eventType, UUID fallbackCorrelationId, String kafkaKey, Object payload) {
        var event = DomainEvent.of(
                eventType,
                "payment-service",
                fallbackCorrelationId,
                payload
        );

        log.info("[Kafka/ALERT] Publishing event={} key={}", eventType, kafkaKey);
        kafkaTemplate.send(topic, kafkaKey, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka/ALERT] FEHLER bei event '{}': {}", eventType, ex.getMessage());
                    }
                });
    }

}