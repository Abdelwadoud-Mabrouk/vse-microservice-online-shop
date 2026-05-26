package com.vseonlineshop.payment.service;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentResponse;
import com.vseonlineshop.payment.dto.ProviderResponse;
import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentTransaction;
import com.vseonlineshop.payment.enums.PaymentStatus;
import com.vseonlineshop.payment.exception.PaymentSessionException;
import com.vseonlineshop.payment.facade.PaymentPluginFacade;
import com.vseonlineshop.payment.repository.PaymentRepository;
import com.vseonlineshop.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final PaymentPluginFacade facade;
    private final KafkaEventPublisher kafkaEventPublisher;


    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        Optional<Payment> existingPayment =
                paymentRepo.findByCorrelationId(request.correlationId());

        if (existingPayment.isPresent()) {
            return handleExistingPayment(existingPayment.get(), request);
        }
        Payment payment = createPendingPayment(request);

        safePublish(() -> kafkaEventPublisher.publishPaymentCreated(payment));

        return createCheckoutSession(payment, request, "SESSION_CREATED", true);
    }

    private PaymentResponse handleExistingPayment(Payment payment, PaymentRequest request) {
        log.warn("Duplicate payment request, correlationId={}", request.correlationId());

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return toResponse(payment);
        }

        log.info("Payment still PENDING. Requesting fresh checkout session.");

        return createCheckoutSession(payment, request, "SESSION_RENEWED", false);
    }

    private PaymentResponse createCheckoutSession(
            Payment payment,
            PaymentRequest request,
            String providerStatus,
            boolean isNewPayment
    ) {
        try {
            ProviderResponse providerResponse = facade.execute(request);

            saveTransaction(
                    payment,
                    providerResponse.providerTransactionId(),
                    providerStatus,
                    null,
                    providerResponse.rawResponse(),
                    0
            );

            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .correlationId(payment.getCorrelationId())
                    .status(payment.getStatus())
                    .approvalUrl(providerResponse.approvalUrl())
                    .paidAt(payment.getPaidAt())
                    .build();

        } catch (Exception ex) {
            return handleProviderFailureIsolated(payment.getPaymentId(), ex, isNewPayment);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse handleProviderFailureIsolated(UUID paymentId, Exception ex, boolean updatePaymentToFailed) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment nicht gefunden: " + paymentId));

        log.error("[PAYMENT] Provider session creation failed. paymentId={}, error={}",
                paymentId, ex.getMessage());

        if (updatePaymentToFailed) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(Instant.now());
            paymentRepo.save(payment);
        }

        PaymentTransaction txn = saveTransaction(
                payment,
                null,
                "PROVIDER_ERROR",
                ex.getMessage(),
                null,
                0
        );

        // Event abschicken
        if (updatePaymentToFailed) {
            safePublish(() -> kafkaEventPublisher.publishPaymentFailed(payment, txn));
        }

        throw new PaymentSessionException("Konnte Zahlungssitzung beim Provider nicht erstellen");
    }

    // ── Helper: Entity → Response ──────────────────────────────────
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .correlationId(payment.getCorrelationId())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }

    // ── Helper: Transaction persistieren ──────────────────────────
    private PaymentTransaction saveTransaction(Payment payment, String providerTxnId,
                                               String providerStatus, String failureReason,
                                               String rawResponse, Integer retryCount) {
        PaymentTransaction txn = PaymentTransaction.builder()
                .payment(payment)
                .providerTransactionId(providerTxnId)
                .providerStatus(providerStatus)
                .failureReason(failureReason)
                .rawResponse(rawResponse)
                .retryCount(retryCount)
                .build();
        return transactionRepo.save(txn);
    }

    private Payment createPendingPayment(PaymentRequest request) {

        Payment payment = Payment.builder()
                .correlationId(request.correlationId())
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.totalAmount())
                .currency(request.currency())
                .paymentProvider(request.paymentProvider())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepo.save(payment);

        log.info("""
                        [PAYMENT]
                        Created paymentId={}
                        status=PENDING
                        """,
                payment.getPaymentId());

        return payment;
    }

    // Kafka fire & forget — Exception darf Payment-Flow nicht brechen!
    private void safePublish(Runnable publishAction) {
        try {
            publishAction.run();
        } catch (Exception e) {
            // todo: Wie können wir damit umgehen, damit wir die Nachvollziehbarkeit nicht verlieren können : Transactional Outbox Pattern.
            log.error("[Kafka] Event konnte nicht gesendet werden: {}", e.getMessage(), e);
        }
    }
}
