package com.vseonlineshop.payment.service;

import com.vseonlineshop.payment.dto.*;
import com.vseonlineshop.payment.dto.refund.RefundProviderResponse;
import com.vseonlineshop.payment.dto.refund.RefundRequestDto;
import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentStatus;
import com.vseonlineshop.payment.entity.PaymentTransaction;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import com.vseonlineshop.payment.exception.PaymentSessionException;
import com.vseonlineshop.payment.strategy.facade.PaymentPluginFacade;
import com.vseonlineshop.payment.kafka.services.KafkaEventPublisher;
import com.vseonlineshop.payment.repository.PaymentRepository;
import com.vseonlineshop.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.vseonlineshop.payment.config.LogUtils.safeContext;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProcessService {

    private final PaymentRepository paymentRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final PaymentPluginFacade paymentFacade;

    public PaymentResponse processPayment(PaymentRequest request) {


        log.info(
                "Payment Prozess gestartet",
                kv("service", "billing-service"),
                kv("context", safeContext(
                        "orderId", request.orderId(),
                        "amount", request.totalAmount()
                ))
        );


        Optional<Payment> existingPayment =
                paymentRepo.findByCorrelationId(request.correlationId());


        if (existingPayment.isPresent()) {

            log.warn(
                    "Payment bereits vorhanden - Idempotenz Check",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "paymentId", existingPayment.get().getPaymentId(),
                            "orderId", request.orderId()
                    ))
            );

            return handleExistingPayment(
                    existingPayment.get(),
                    request
            );
        }


        Payment payment = createPendingPayment(request);


        safePublish(() ->
                kafkaEventPublisher.publishPaymentCreated(payment)
        );


        return createCheckoutSession(
                payment,
                request,
                "SESSION_CREATED",
                true
        );


    }


    public RefundProviderResponse processRefund(RefundRequestDto refundRequestDto, UUID correlationId) {


        log.info(
                "Refund Prozess gestartet",
                kv("service", "billing-service"),
                kv("context", safeContext(
                        "amount", refundRequestDto.amount(),
                        "currency", refundRequestDto.currency()
                ))
        );

        // 1. Payment Entity laden
        Payment payment = paymentRepo.findByCorrelationId(correlationId)
                .orElseThrow(() -> new PaymentSessionException("Keine Zahlung für CorrelationId " + correlationId + " gefunden"));

        // ============================
        // IDEMPOTENZ CHECK
        // ============================

        if (payment.getStatus() == PaymentStatus.REFUND_SUCCEEDED) {

            log.warn(
                    "Refund bereits erfolgreich verarbeitet",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "paymentId", payment.getPaymentId(),
                            "refundTransactionId", payment.getRefundTransactionId()
                    ))
            );
            return RefundProviderResponse.builder().refundId(payment.getRefundTransactionId()).paymentStatus(PaymentStatus.REFUND_SUCCEEDED).build();

        }

        if (payment.getStatus() == PaymentStatus.REFUND_INITIALIZED
                || payment.getStatus() == PaymentStatus.REFUND_FAILED) {


            log.warn(
                    "Refund bereits in Verarbeitung",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "paymentId", payment.getPaymentId(),
                            "status", payment.getStatus()
                    ))
            );


            throw new PaymentServiceException(
                    "Refund wird bereits verarbeitet"
            );
        }

        PaymentTransaction transaction =
                transactionRepo
                        .findPaymentTransactionsByPayment(payment)
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new PaymentServiceException(
                                        "Keine Transaktion gefunden"
                                )
                        );


        payment.setStatus(
                PaymentStatus.REFUND_INITIALIZED
        );

        paymentRepo.save(payment);


        safePublish(() ->
                kafkaEventPublisher.publishRefundInitialized(
                        payment,
                        refundRequestDto.amount()
                )
        );

        try {


            RefundProviderResponse refundProviderResponse = paymentFacade.refund(transaction.getProviderTransactionId(), refundRequestDto.amount(), refundRequestDto.currency(), refundRequestDto.method());

            // 5. Status auf REFUND_SUCCEEDED setzen, Refund-ID speichern & Erfolgsevent feuern
            payment.setStatus(PaymentStatus.REFUND_SUCCEEDED);
            payment.setRefundTransactionId(refundProviderResponse.refundId());
            paymentRepo.save(payment);

            log.info(
                    "Refund erfolgreich abgeschlossen",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "paymentId", payment.getPaymentId(),
                            "refundTransactionId", refundProviderResponse.paymentStatus()
                    ))
            );

            safePublish(() ->
                    kafkaEventPublisher.publishRefundSucceeded(
                            payment,
                            refundProviderResponse.refundId(),
                            refundRequestDto.amount()
                    )
            );


            return refundProviderResponse;
        } catch (Exception e) {


            log.error(
                    "Refund fehlgeschlagen",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "paymentId", payment.getPaymentId(),
                            "errorMessage", e.getMessage()
                    )),
                    e
            );

            // 6. Bei Fehler: Status anpassen & Fehlerevent für Audit/Saga feuern
            payment.setStatus(PaymentStatus.REFUND_FAILED);
            paymentRepo.save(payment);
            safePublish(() -> kafkaEventPublisher.publishRefundFailed(payment, e.getMessage()));

            throw new PaymentServiceException("Rückerstattung fehlgeschlagen: " + e.getMessage());
        }


    }

    public PaymentResponse getByCorrelationId(UUID correlationId) {
        log.info(
                "Abfrage des Zahlungsstatus per Correlation-ID",
                kv("service", "billing-service"),
                kv("context", safeContext("correlationId", correlationId))
        );

        return paymentRepo.findByCorrelationId(correlationId)
                .map(this::mapToPaymentResponse)
                .orElseThrow(() -> {
                    log.warn(
                            "Keine Zahlung zur Correlation-ID gefunden",
                            kv("service", "billing-service"),
                            kv("context", safeContext("correlationId", correlationId))
                    );
                    return new PaymentServiceException("Payment mit Correlation-ID " + correlationId + " wurde nicht gefunden.");
                });
    }


    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .correlationId(payment.getCorrelationId())
                .orderId(payment.getOrderId())
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }

    public PaymentStatusResponse getStatus(String transactionId) {


        try {

            log.info(
                    "Payment Statusabfrage gestartet",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "transactionId", transactionId
                    ))
            );

            return paymentFacade.getStatus(transactionId);

        } catch (Exception ex) {

            log.error(
                    "Payment Statusabfrage fehlgeschlagen",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "transactionId", transactionId,
                            "errorMessage", ex.getMessage()
                    )),
                    ex
            );

            throw ex;
        }
    }

    public PaymentResponse handleExistingPayment(Payment payment, PaymentRequest request) {
        log.warn(
                "Duplicate Payment Request - Idempotency Check",
                kv("context", safeContext(
                        "paymentId", payment.getPaymentId(),
                        "orderId", request.orderId()
                ))
        );

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return toResponse(payment);
        }

        log.info(
                "Payment is still pending - creating new checkout session",
                kv("context", safeContext(
                        "paymentId", payment.getPaymentId(),
                        "orderId", payment.getOrderId()
                ))
        );

        return createCheckoutSession(payment, request, "SESSION_RENEWED", false);
    }

    public PaymentResponse createCheckoutSession(
            Payment payment,
            PaymentRequest request,
            String providerStatus,
            boolean isNewPayment
    ) {
        try {
            PaymentProviderResponse providerResponse = paymentFacade.charge(request, payment.getPaymentId());
            saveTransaction(
                    payment,
                    providerResponse.providerTransactionId(),
                    providerStatus,
                    null,
                    providerResponse.rawResponse(),
                    0
            );

            // Rückgabe an den Controller mit dem Builder
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


    public PaymentResponse handleProviderFailureIsolated(UUID paymentId, Exception ex, boolean updatePaymentToFailed) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment nicht gefunden: " + paymentId));

        log.error(
                "Payment Provider Session Erstellung fehlgeschlagen",
                kv("context", safeContext(
                        "paymentId", paymentId,
                        "errorMessage", ex.getMessage()
                )),
                ex
        );

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

    public Payment createPendingPayment(PaymentRequest request) {

        Payment payment = Payment.builder()
                .correlationId(request.correlationId())
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.totalAmount())
                .currency(request.currency())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepo.save(payment);

        log.info(
                "Payment erfolgreich erstellt",
                kv("context", safeContext(
                        "paymentId", payment.getPaymentId(),
                        "orderId", payment.getOrderId(),
                        "status", payment.getStatus(),
                        "amount", payment.getAmount()
                ))
        );

        return payment;
    }

    public void safePublish(Runnable publishAction) {
        try {
            publishAction.run();
        } catch (Exception e) {
            log.error(
                    "Kafka Event konnte nicht gesendet werden",
                    kv("context", safeContext(
                            "errorMessage", e.getMessage()
                    )),
                    e
            );
        }
    }
}
