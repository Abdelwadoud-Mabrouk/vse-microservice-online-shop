package com.vseonlineshop.payment.webhook;

import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentTransaction;
import com.vseonlineshop.payment.enums.PaymentStatus;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import com.vseonlineshop.payment.repository.PaymentRepository;
import com.vseonlineshop.payment.repository.PaymentTransactionRepository;
import com.vseonlineshop.payment.service.KafkaEventPublisher;
import com.vseonlineshop.payment.webhook.papal.PaypalWebhookPayload;
import com.vseonlineshop.payment.webhook.stripe.StripeWebhookPayload;
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
public class WebhookService {

    private final PaymentRepository paymentRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handlePaypalWebhook(PaypalWebhookPayload payload) {

        String eventType = payload.eventType();
        String providerTxnId = payload.resource().providerTransactionId();
        String paymentIdRaw = payload.resource().customId();

        log.info("[WEBHOOK/PAYPAL] event={} providerTxn={} paymentId={}",
                eventType, providerTxnId, paymentIdRaw);

        // ── Payment in DB laden ────────────────────────────
        Payment payment = paymentRepo.findById(UUID.fromString(paymentIdRaw))
                .orElseThrow(() -> {
                    log.error("[WEBHOOK/PAYPAL] Payment nicht gefunden paymentId={}", paymentIdRaw);
                    return new PaymentServiceException("Payment not found: " + paymentIdRaw);
                });

        // ── Idempotenz: schon verarbeitet? ─────────────────
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("[WEBHOOK/PAYPAL] Bereits verarbeitet paymentId={} status={}",
                    paymentIdRaw, payment.getStatus());
            return;
        }

        // ── rawResponse als JSON speichern (Audit) ─────────
        String rawResponse = toJson(payload);

        // ── Event-Type auswerten ───────────────────────────
        switch (eventType) {
            case "PAYMENT.CAPTURE.COMPLETED" -> handleSuccess(payment, providerTxnId, rawResponse, Instant.now());
            case "PAYMENT.CAPTURE.DENIED", "PAYMENT.CAPTURE.DECLINED" -> {
                String failureReason = payload.resource().statusDetails() != null
                        ? payload.resource().statusDetails().reason()
                        : eventType;
            }
        }
    }

    @Transactional
    public void handleStripeWebhook(StripeWebhookPayload payload) {

        String eventType = payload.type();
        StripeWebhookPayload.StripeObject stripeObj = payload.data().object();
        String providerTxnId = stripeObj.id();
        String paymentIdRaw = stripeObj.metadata() != null ? stripeObj.metadata().get("paymentId") : null;

        log.info("[WEBHOOK/STRIPE] event={} providerTxn={} paymentId={}", eventType, providerTxnId, paymentIdRaw);

        if (paymentIdRaw == null || paymentIdRaw.isBlank()) {
            log.error("[WEBHOOK/STRIPE] Webhook enthält keine interne paymentId! providerTxn={}", providerTxnId);
            // ERGÄNZUNG 1: System-Fehler an Audit/Kafka melden
            safePublish(() -> kafkaEventPublisher.publishSystemAlert(providerTxnId, "Webhook missing paymentId. Txn: " + providerTxnId));
            return;
        }

        // ── Payment in DB laden ────────────────────────────
        Optional<Payment> paymentOpt = paymentRepo.findById(UUID.fromString(paymentIdRaw));
        if (paymentOpt.isEmpty()) {
            log.error("[WEBHOOK/STRIPE] Payment nicht gefunden paymentId={}", paymentIdRaw);
            // ERGÄNZUNG 2: Sicherheits-Alarm / Audit-Event abfeuern vor dem Abbruch!
            safePublish(() -> kafkaEventPublisher.publishSecurityAlert(
                    paymentIdRaw,
                    "Unknown paymentId in Webhook: " + paymentIdRaw
            ));
            throw new PaymentServiceException("Payment not found: " + paymentIdRaw);
        }

        Payment payment = paymentOpt.get();

        // ── Idempotenz: schon verarbeitet? ─────────────────
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("[WEBHOOK/STRIPE] Bereits verarbeitet paymentId={} status={}", paymentIdRaw, payment.getStatus());
            // ERGÄNZUNG 3: Event für den Audit-Service (Duplikat-Protokoll)
            safePublish(() -> kafkaEventPublisher.publishWebhookDuplicateIgnored(payment));
            return;
        }

        String rawResponse = toJson(payload);

        switch (eventType) {
            case "payment_intent.succeeded" -> handleSuccess(payment, providerTxnId, rawResponse, Instant.now());
            case "payment_intent.payment_failed" ->
                    handleFailure(payment, providerTxnId, "Stripe card declined", rawResponse, Instant.now());

            // ERGÄNZUNG 4: Informative Events mitschreiben (z.B. wenn Stripe meldet, dass die Zahlung verarbeitet wird)
            default -> handleInformativeEvent(payment, providerTxnId, eventType, rawResponse);
        }
    }


    // Neue Helper-Methode für unkritische/andere Stripe-Events
    private void handleInformativeEvent(Payment payment, String providerTxnId, String eventType, String rawResponse) {
        log.info("[WEBHOOK/INFO] Non-final event '{}' received for paymentId={}", eventType, payment.getPaymentId());

        // Speichern es in der Historie (Audit)
        PaymentTransaction txn = saveTransaction(payment, providerTxnId, eventType, null, rawResponse);

        // Funken an Kafka, damit das Audit-Log weiß, was passiert ist
        safePublish(() -> kafkaEventPublisher.publishPaymentNotificationReceived(payment, txn, eventType));
    }

    // ── Erfolg ─────────────────────────────────────────────
    private void handleSuccess(Payment payment,
                               String providerTxnId,
                               String rawResponse, Instant timestamp) {

        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setPaidAt(timestamp);
        payment.setUpdatedAt(timestamp);
        paymentRepo.save(payment);

        // Transaktion-Historie schreiben
        PaymentTransaction txn = saveTransaction(
                payment,
                providerTxnId,
                "COMPLETED",
                null,
                rawResponse
        );

        log.info("[WEBHOOK/SUCCESS] CONFIRMED paymentId={} via {}",
                payment.getPaymentId(), payment.getPaymentProvider());

        // Kafka Event abfeuern (Shop, Notification, Audit hören zu)
        safePublish(() -> kafkaEventPublisher.publishPaymentSucceeded(payment, txn));
    }

    // ── Fehler ─────────────────────────────────────────────
    private void handleFailure(Payment payment,
                               String providerTxnId,
                               String failureReason,
                               String rawResponse, Instant timestamp) {

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(timestamp);
        Payment savedPayment = paymentRepo.save(payment);

        // Transaktion-Historie schreiben
        PaymentTransaction txn = saveTransaction(
                payment,
                providerTxnId,
                "FAILED",
                failureReason,
                rawResponse
        );

        log.warn("[WEBHOOK/FAILURE] FAILED paymentId={} reason={} via {}",
                payment.getPaymentId(), failureReason, payment.getPaymentProvider());

        // Kafka Event für Saga-Kompensation (Shop rollback, Notification Fehler-Mail)
        safePublish(() -> kafkaEventPublisher.publishPaymentFailed(savedPayment, txn));
    }

    // ── Helpers ────────────────────────────────────────────
    private PaymentTransaction saveTransaction(Payment payment, String providerTxnId,
                                               String status, String failureReason,
                                               String rawResponse) {
        return transactionRepo.save(PaymentTransaction.builder()
                .payment(payment)
                .providerTransactionId(providerTxnId)
                .providerStatus(status)
                .failureReason(failureReason)
                .rawResponse(rawResponse)
                .retryCount(0)
                .build());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);

        } catch (Exception e) {
            return obj.toString();
        }
    }

    private void safePublish(Runnable fn) {
        try {
            fn.run();
        } catch (Exception ex) {
            log.error("[Kafka] safePublish fehlgeschlagen: {}", ex.getMessage(), ex);
        }
    }
}
