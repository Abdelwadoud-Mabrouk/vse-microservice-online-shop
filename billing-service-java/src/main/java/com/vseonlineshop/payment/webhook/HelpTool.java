package com.vseonlineshop.payment.webhook;

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

@Service
@AllArgsConstructor
@Slf4j
public class HelpTool {

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

        log.info("[WEBHOOK/PAYPAL] event={} providerTxn={} paymentId={} orderId={} correlationId={}",
                eventType, checkoutSessionId, extractedPaymentId, extractedOrderId, extractedCorrelationId);

        return new WebHookMetadata(extractedPaymentId, extractedOrderId, extractedCorrelationId);
    }

    public PaymentIntent getPaymentIntent(Event event, StripeObject stripeObject) {
        String eventType = event.getType();
        if (!eventType.equals("payment_intent.succeeded") && !eventType.equals("payment_intent.payment_failed")) {
            log.info("[WEBHOOK/STRIPE] Event ignoriert: {}", eventType);
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
            log.error("[WEBHOOK] Webhook enthält keine interne paymentId! providerTxn={}", checkoutSessionId);
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(checkoutSessionId, "Webhook missing paymentId. Txn: " + checkoutSessionId));
            throw new WebhookProcessingException(
                    "Webhook missing paymentId for txn: " + checkoutSessionId
            );
        }
        if (metadata.orderId() == null || metadata.orderId().isBlank() || metadata.correlationId() == null || metadata.correlationId().isBlank()) {
            log.error("[WEBHOOK] Webhook enthält keine orderId oder correlationId! providerTxn={}", checkoutSessionId);
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(checkoutSessionId, "Webhook missing orderId/correlationId. Txn: " + checkoutSessionId));
            throw new WebhookProcessingException(
                    "Webhook missing orderId/correlationId for txn: " + checkoutSessionId
            );
        }
    }

    public Payment loadAndVerifyPayment(WebHookMetadata metadata) {
        Optional<Payment> paymentOpt = paymentRepo.findById(UUID.fromString(metadata.paymentIdRaw()));
        if (paymentOpt.isEmpty()) {
            log.error("[WEBHOOK] Payment nicht gefunden paymentId={}", metadata.paymentIdRaw());
            safePublish(() -> kafkaEventPublisher.publishSecurityAlert(metadata.paymentIdRaw(), "Unknown paymentId in Webhook: " + metadata.paymentIdRaw()));
            throw new WebhookProcessingException("Payment not found: " + metadata.paymentIdRaw());
        }

        Payment payment = paymentOpt.get();

        if (!metadata.orderId().equals(payment.getOrderId().toString()) ||
                !metadata.correlationId().equals(payment.getCorrelationId().toString())) {

            log.error("[WEBHOOK] Metadata stimmt nicht mit DB überein! erwartet orderId={} correlationId={} | bekommen orderId={} correlationId={}",
                    payment.getOrderId(), payment.getCorrelationId(), metadata.orderId(), metadata.correlationId());
            safePublish(() -> kafkaEventPublisher.publishSecurityAlert(metadata.paymentIdRaw(), "Metadata mismatch for paymentId: " + metadata.paymentIdRaw()));
            throw new WebhookSignatureException("Metadata-Mismatch für paymentId: " + metadata.paymentIdRaw());
        }
        return payment;
    }

    public void handleSuccess(Payment payment, String providerTxnId, String capturedId, String rawResponse, Instant timestamp) {
        log.info(
                "ENTER handleSuccess paymentId={} status={} providerTxnId={} capturedId={}",
                payment.getPaymentId(),
                payment.getStatus(),
                providerTxnId,
                capturedId
        );
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setPaidAt(timestamp);
        payment.setUpdatedAt(timestamp);

        paymentRepo.saveAndFlush(payment);
        PaymentTransaction txn = saveTransaction(payment, providerTxnId, capturedId, "COMPLETED", null, rawResponse);

        log.info("[WEBHOOK/SUCCESS] CONFIRMED paymentId={} via {}", payment.getPaymentId(), payment.getPaymentMethod());
        safePublish(() -> kafkaEventPublisher.publishPaymentSucceeded(payment, txn));
    }

    public void handleFailure(Payment payment, String providerTxnId, String failureReason, String rawResponse, Instant timestamp) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(timestamp);

        Payment savedPayment = paymentRepo.saveAndFlush(payment);
        PaymentTransaction txn = saveTransaction(payment, providerTxnId, null,"FAILED", failureReason, rawResponse);

        log.warn("[WEBHOOK/FAILURE] FAILED paymentId={} reason={} via {}", payment.getPaymentId(), failureReason, payment.getPaymentMethod());
        safePublish(() -> kafkaEventPublisher.publishPaymentFailed(savedPayment, txn));
    }

    public void handleInformativeEvent(Payment payment, String providerTxnId, String capturedId, String eventType, String rawResponse) {
        log.info("[WEBHOOK/INFO] Non-final event '{}' received for paymentId={}", eventType, payment.getPaymentId());
        PaymentTransaction txn = saveTransaction(payment, providerTxnId, capturedId, eventType, null, rawResponse);
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
        } catch (Exception ex) {
            log.error("[Kafka] safePublish fehlgeschlagen: {}", ex.getMessage(), ex);
        }
    }

    public String toJson(Object obj) {
        if (obj instanceof com.stripe.model.StripeObject) {
            return ((com.stripe.model.StripeObject) obj).toJson();
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("[WEBHOOK/JSON] Konnte Objekt nicht serialisieren", e);
            return "{}";
        }
    }

    public void systemAlarm() {
        safePublish(() -> kafkaEventPublisher.publishSystemAlert(null, "Internal Error " + null));
    }
}
