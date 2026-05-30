package com.vseonlineshop.payment.webhook.stripe;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentTransaction;
import com.vseonlineshop.payment.enums.PaymentStatus;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import com.vseonlineshop.payment.repository.PaymentRepository;
import com.vseonlineshop.payment.repository.PaymentTransactionRepository;
import com.vseonlineshop.payment.service.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {


    private final PaymentRepository paymentRepo;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional
    public void handleStripeWebhook(Event event, StripeObject stripeObject) {
        String eventType = event.getType();

        if (!eventType.equals("payment_intent.succeeded") &&
                !eventType.equals("payment_intent.payment_failed")) {
            log.info("[WEBHOOK/STRIPE] Event ignoriert: {}", eventType);
            return;
        }

        if (stripeObject == null) {
            log.error("[WEBHOOK/STRIPE] Deserialisierung fehlgeschlagen event={}", eventType);
            return;
        }


        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;

        // 1. Metadaten & Extrahierung
        StripeMetadata metadata = extractMetadata(paymentIntent);
        String checkoutSessionId = extractCheckoutSessionId(paymentIntent);

        log.info("[WEBHOOK/STRIPE] event={} providerTxn={} paymentId={} orderId={} correlationId={}",
                eventType, checkoutSessionId, metadata.paymentIdRaw(), metadata.orderId(), metadata.correlationId());

        // 2. Validierung der Pflichtfelder
        if (!validateRequiredFields(metadata, checkoutSessionId)) {
            return;
        }

        // 3. Laden und Konsistenzprüfung des Payments
        Payment payment = loadAndVerifyPayment(metadata);
        if (payment == null) {
            return;
        }

        // 4. Idempotenz-Prüfung
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("[WEBHOOK/STRIPE] Bereits verarbeitet paymentId={} status={}", metadata.paymentIdRaw(), payment.getStatus());
            safePublish(() -> kafkaEventPublisher.publishWebhookDuplicateIgnored(payment));
            return;
        }

        String rawResponse = toJson(event);

        // 5. Routing des Event-Typs
        switch (eventType) {
            case "payment_intent.succeeded" -> handleSuccess(payment, checkoutSessionId, rawResponse, Instant.now());
            case "payment_intent.payment_failed" ->
                    handleFailure(payment, checkoutSessionId, "Stripe card declined", rawResponse, Instant.now());
            default -> handleInformativeEvent(payment, checkoutSessionId, eventType, rawResponse);
        }
    }

    // ── HILFSMETHODEN FÜR DIE LOGISCHE TRENNUNG ───────────────────────────

    private StripeMetadata extractMetadata(PaymentIntent paymentIntent) {
        var meta = paymentIntent.getMetadata();
        return new StripeMetadata(
                meta != null ? meta.get("paymentId") : null,
                meta != null ? meta.get("orderId") : null,
                meta != null ? meta.get("correlationId") : null
        );
    }

    private String extractCheckoutSessionId(PaymentIntent paymentIntent) {
        return paymentIntent.getPaymentDetails() != null
                ? paymentIntent.getPaymentDetails().getOrderReference()
                : null;
    }

    private boolean validateRequiredFields(StripeMetadata metadata, String checkoutSessionId) {
        if (metadata.paymentIdRaw() == null || metadata.paymentIdRaw().isBlank()) {
            log.error("[WEBHOOK/STRIPE] Webhook enthält keine interne paymentId! providerTxn={}", checkoutSessionId);
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(checkoutSessionId, "Webhook missing paymentId. Txn: " + checkoutSessionId));
            return false;
        }
        if (metadata.orderId() == null || metadata.orderId().isBlank() || metadata.correlationId() == null || metadata.correlationId().isBlank()) {
            log.error("[WEBHOOK/STRIPE] Webhook enthält keine orderId oder correlationId! providerTxn={}", checkoutSessionId);
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(checkoutSessionId, "Webhook missing orderId/correlationId. Txn: " + checkoutSessionId));
            return false;
        }
        return true;
    }

    private Payment loadAndVerifyPayment(StripeMetadata metadata) {
        Optional<Payment> paymentOpt = paymentRepo.findById(UUID.fromString(metadata.paymentIdRaw()));
        if (paymentOpt.isEmpty()) {
            log.error("[WEBHOOK/STRIPE] Payment nicht gefunden paymentId={}", metadata.paymentIdRaw());
            safePublish(() -> kafkaEventPublisher.publishSecurityAlert(metadata.paymentIdRaw(), "Unknown paymentId in Webhook: " + metadata.paymentIdRaw()));
            throw new PaymentServiceException("Payment not found: " + metadata.paymentIdRaw());
        }

        Payment payment = paymentOpt.get();

        if (!metadata.orderId().equals(payment.getOrderId().toString()) ||
                !metadata.correlationId().equals(payment.getCorrelationId().toString())) {

            log.error("[WEBHOOK/STRIPE] Metadata stimmt nicht mit DB überein! erwartet orderId={} correlationId={} | bekommen orderId={} correlationId={}",
                    payment.getOrderId(), payment.getCorrelationId(), metadata.orderId(), metadata.correlationId());
            safePublish(() -> kafkaEventPublisher.publishSecurityAlert(metadata.paymentIdRaw(), "Metadata mismatch for paymentId: " + metadata.paymentIdRaw()));
            return null;
        }
        return payment;
    }

    private void handleSuccess(Payment payment, String providerTxnId, String rawResponse, Instant timestamp) {
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setPaidAt(timestamp);
        payment.setUpdatedAt(timestamp);

        paymentRepo.saveAndFlush(payment);

        PaymentTransaction txn = saveTransaction(payment, providerTxnId, "COMPLETED", null, rawResponse);

        log.info("[WEBHOOK/SUCCESS] CONFIRMED paymentId={} via {}", payment.getPaymentId(), payment.getPaymentMethod());

        // Kafka wird erst getriggert, wenn die DB-Zeilen oben erfolgreich geschrieben wurden
        safePublish(() -> kafkaEventPublisher.publishPaymentSucceeded(payment, txn));
    }

    private void handleFailure(Payment payment, String providerTxnId, String failureReason, String rawResponse, Instant timestamp) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(timestamp);

        // SICHERHEIT: Sofortiges Schreiben erzwingen
        Payment savedPayment = paymentRepo.saveAndFlush(payment);

        PaymentTransaction txn = saveTransaction(payment, providerTxnId, "FAILED", failureReason, rawResponse);

        log.warn("[WEBHOOK/FAILURE] FAILED paymentId={} reason={} via {}", payment.getPaymentId(), failureReason, payment.getPaymentMethod());
        safePublish(() -> kafkaEventPublisher.publishPaymentFailed(savedPayment, txn));
    }

    private void handleInformativeEvent(Payment payment, String providerTxnId, String eventType, String rawResponse) {
        log.info("[WEBHOOK/INFO] Non-final event '{}' received for paymentId={}", eventType, payment.getPaymentId());
        PaymentTransaction txn = saveTransaction(payment, providerTxnId, eventType, null, rawResponse);
        safePublish(() -> kafkaEventPublisher.publishPaymentNotificationReceived(payment, txn, eventType));
    }

    private PaymentTransaction saveTransaction(Payment payment, String providerTxnId, String status, String failureReason, String rawResponse) {
        // SICHERHEIT: saveAndFlush stellt sicher, dass auch die Historie sofort fest in der DB verankert ist
        return paymentTransactionRepository.saveAndFlush(PaymentTransaction.builder()
                .payment(payment)
                .providerTransactionId(providerTxnId)
                .providerStatus(status)
                .failureReason(failureReason)
                .rawResponse(rawResponse)
                .retryCount(0)
                .build());
    }


    private void safePublish(Runnable fn) {
        try {
            fn.run();
        } catch (Exception ex) {
            log.error("[Kafka] safePublish fehlgeschlagen: {}", ex.getMessage(), ex);
        }
    }

    private String toJson(Object obj) {
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

    private record StripeMetadata(String paymentIdRaw, String orderId, String correlationId) {
    }
}