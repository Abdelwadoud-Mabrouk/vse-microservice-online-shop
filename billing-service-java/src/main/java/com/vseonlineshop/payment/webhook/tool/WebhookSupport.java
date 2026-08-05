package com.vseonlineshop.payment.webhook.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.vseonlineshop.payment.dto.webhook.WebHookMetadata;
import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentTransaction;
import com.vseonlineshop.payment.entity.PaymentStatus;
import com.vseonlineshop.payment.repository.PaymentRepository;
import com.vseonlineshop.payment.repository.PaymentTransactionRepository;
import com.vseonlineshop.payment.kafka.services.KafkaEventPublisher;
import com.vseonlineshop.payment.webhook.exception.WebhookProcessingException;
import com.vseonlineshop.payment.webhook.exception.WebhookSignatureException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.vseonlineshop.payment.config.LogUtils.safeContext;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@AllArgsConstructor
@Slf4j
public class WebhookSupport {

    private final PaymentRepository paymentRepo;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebHookMetadata getMetadata(String customIdRaw, String eventType, String checkoutSessionId) {
        String extractedPaymentId = null;
        String extractedOrderId = null;
        String extractedCorrelationId = null;

        if (!customIdRaw.isEmpty() && customIdRaw.contains("|")) {
            String[] parts = customIdRaw.split("\\|");
            for (String part : parts) {
                if (part.startsWith("paymentId=")) extractedPaymentId = part.replace("paymentId=", "");
                if (part.startsWith("orderId=")) extractedOrderId = part.replace("orderId=", "");
                if (part.startsWith("correlationId=")) extractedCorrelationId = part.replace("correlationId=", "");
            }
        }

        log.debug(
                "Webhook Metadaten extrahiert",
                kv("service", "billing-service"),
                kv("context", safeContext(
                        "paymentId", extractedPaymentId,
                        "orderId", extractedOrderId,
                        "eventType", eventType,
                        "checkoutSessionId", checkoutSessionId
                ))
        );

        return new WebHookMetadata(extractedPaymentId, extractedOrderId, extractedCorrelationId);
    }

    public PaymentIntent getPaymentIntent(Event event, StripeObject stripeObject) {
        String eventType = event.getType();
        if (!eventType.equals("payment_intent.succeeded") && !eventType.equals("payment_intent.payment_failed")) {
            log.debug(
                    "Stripe Webhook Event nicht relevant",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "stripe",
                            "eventType", eventType
                    ))
            );
            return null;
        }
        return (PaymentIntent) stripeObject;
    }

    public WebHookMetadata extractMetadata(PaymentIntent paymentIntent) {
        var meta = paymentIntent.getMetadata();
        return new WebHookMetadata(
                meta != null ? meta.get("paymentId") : null,
                meta != null ? meta.get("orderId") : null,
                meta != null ? meta.get("correlationId") : null
        );

    }

    public String extractCheckoutSessionId(PaymentIntent paymentIntent) {
        if (paymentIntent.getPaymentDetails() != null && paymentIntent.getPaymentDetails().getOrderReference() != null) {
            return paymentIntent.getPaymentDetails().getOrderReference();
        }
        return paymentIntent.getId();
    }


    public void validateRequiredFields(WebHookMetadata metadata, String checkoutSessionId) {
        if (metadata.paymentIdRaw() == null || metadata.paymentIdRaw().isBlank()) {
            log.error(
                    "Webhook Pflichtfeld paymentId fehlt",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "providerTransactionId", checkoutSessionId
                    ))
            );
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(checkoutSessionId, "Webhook missing paymentId. Txn: " + checkoutSessionId));
            throw new WebhookProcessingException(
                    "Webhook missing paymentId for txn: " + checkoutSessionId
            );
        }
        if (metadata.orderId() == null || metadata.orderId().isBlank() || metadata.correlationId() == null || metadata.correlationId().isBlank()) {
            log.error(
                    "Webhook Pflichtdaten fehlen",
                    kv("service","billing-service"),
                    kv("context",safeContext(
                            "providerTransactionId", checkoutSessionId,
                            "missingFields", "orderId/correlationId"
                    ))
            );
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(checkoutSessionId, "Webhook missing orderId/correlationId. Txn: " + checkoutSessionId));
            throw new WebhookProcessingException(
                    "Webhook missing orderId/correlationId for txn: " + checkoutSessionId
            );
        }
    }

    public Payment loadAndVerifyPayment(WebHookMetadata metadata) {
        Optional<Payment> paymentOpt = paymentRepo.findById(UUID.fromString(metadata.paymentIdRaw()));
        if (paymentOpt.isEmpty()) {
            log.error(
                    "Payment für Webhook nicht gefunden",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "paymentId", metadata.paymentIdRaw()
                    ))
            );
            safePublish(() -> kafkaEventPublisher.publishSecurityAlert(metadata.paymentIdRaw(), "Unknown paymentId in Webhook: " + metadata.paymentIdRaw()));
            throw new WebhookProcessingException("Payment not found: " + metadata.paymentIdRaw());
        }

        Payment payment = paymentOpt.get();

        if (!metadata.orderId().equals(payment.getOrderId().toString()) ||
                !metadata.correlationId().equals(payment.getCorrelationId().toString())) {

            log.error(
                    "Webhook Security Validierung fehlgeschlagen",
                    kv("service","billing-service"),
                    kv("context",safeContext(
                            "paymentId", metadata.paymentIdRaw(),
                            "reason","metadata_mismatch",
                            "expectedOrderId", payment.getOrderId(),
                            "receivedOrderId", metadata.orderId()
                    ))
            );
            safePublish(() -> kafkaEventPublisher.publishSecurityAlert(metadata.paymentIdRaw(), "Metadata mismatch for paymentId: " + metadata.paymentIdRaw()));
            throw new WebhookSignatureException("Metadata-Mismatch für paymentId: " + metadata.paymentIdRaw());
        }
        return payment;
    }

    public void handleSuccess(Payment payment, String providerTxnId, String capturedId, String rawResponse, Instant timestamp) {

        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setPaidAt(timestamp);
        payment.setUpdatedAt(timestamp);

        paymentRepo.saveAndFlush(payment);
        PaymentTransaction txn = saveTransaction(payment, providerTxnId, capturedId, "COMPLETED", null, rawResponse);

        log.info(
                "Payment Status aktualisiert",
                kv("service", "billing-service"),
                kv("context", safeContext(
                        "paymentId", payment.getPaymentId(),
                        "newStatus", "CONFIRMED"
                ))
        );
        safePublish(() -> kafkaEventPublisher.publishPaymentSucceeded(payment, txn));
    }

    public void handleFailure(Payment payment, String providerTxnId, String failureReason, String rawResponse, Instant timestamp) {

        PaymentStatus oldStatus = payment.getStatus();

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(timestamp);

        Payment savedPayment = paymentRepo.saveAndFlush(payment);
        PaymentTransaction txn = saveTransaction(payment, providerTxnId, null, "FAILED", failureReason, rawResponse);

        log.warn(
                "Payment Status auf FAILED geändert",
                kv("service", "billing-service"),
                kv("context", safeContext(
                        "paymentId", payment.getPaymentId(),
                        "paymentMethod", payment.getPaymentMethod(),
                        "oldStatus", oldStatus,
                        "newStatus", "FAILED",
                        "providerTransactionId", providerTxnId,
                        "failureReason", failureReason
                ))
        );
        safePublish(() -> kafkaEventPublisher.publishPaymentFailed(savedPayment, txn));
    }

    public void handleInformativeEvent(Payment payment, String providerTxnId, String capturedId, String eventType, String rawResponse) {
        log.info("[WEBHOOK/INFO] Non-final event '{}' received for paymentId={}", eventType, payment.getPaymentId());
        PaymentTransaction txn = saveTransaction(
                payment,
                providerTxnId,
                capturedId,
                eventType,
                null,
                rawResponse
        );

        log.debug(
                "Nicht finales Payment Event gespeichert",
                kv("service", "billing-service"),
                kv("context", safeContext(
                        "paymentId", payment.getPaymentId(),
                        "paymentMethod", payment.getPaymentMethod(),
                        "eventType", eventType,
                        "providerTransactionId", providerTxnId
                ))
        );
        safePublish(() -> kafkaEventPublisher.publishPaymentNotificationReceived(payment, txn, eventType));
    }

    private PaymentTransaction saveTransaction(Payment payment, String providerTxnId, String capturedId, String status, String failureReason, String rawResponse) {
        return paymentTransactionRepository.saveAndFlush(PaymentTransaction.builder()
                .payment(payment)
                .providerTransactionId(providerTxnId)
                .captureId(capturedId)
                .providerStatus(status)
                .failureReason(failureReason)
                .rawResponse(rawResponse)
                .retryCount(0)
                .build());
    }

    public void safePublish(Runnable fn) {
        try {
            fn.run();

            log.info(
                    "Kafka Event erfolgreich veröffentlicht",
                    kv("service","billing-service"),
                    kv("context",safeContext(
                            "eventStatus","published"
                    ))
            );
        } catch (Exception ex) {
            log.error(
                    "Kafka Event Veröffentlichung fehlgeschlagen",
                    kv("service","billing-service"),
                    kv("context",safeContext(
                            "errorType",ex.getClass().getSimpleName(),
                            "errorMessage",ex.getMessage()
                    )),
                    ex
            );
        }
    }

    public String toJson(Object obj) {
        if (obj instanceof com.stripe.model.StripeObject) {
            return ((com.stripe.model.StripeObject) obj).toJson();
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error(
                    "Webhook Objekt Serialisierung fehlgeschlagen",
                    kv("service","billing-service"),
                    kv("context",safeContext(
                            "objectType",obj.getClass().getSimpleName(),
                            "errorType",e.getClass().getSimpleName()
                    )),
                    e
            );
            return "{}";
        }
    }

    public void systemAlarm() {
        safePublish(() -> kafkaEventPublisher.publishSystemAlert(null, "Internal Error " + null));
    }
}
