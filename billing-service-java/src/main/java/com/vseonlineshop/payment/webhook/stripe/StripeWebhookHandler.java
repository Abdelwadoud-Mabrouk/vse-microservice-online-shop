package com.vseonlineshop.payment.webhook.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.vseonlineshop.payment.dto.webhook.WebHookMetadata;
import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentStatus;
import com.vseonlineshop.payment.kafka.services.KafkaEventPublisher;
import com.vseonlineshop.payment.webhook.tool.WebhookSupport;
import com.vseonlineshop.payment.webhook.WebhookHandler;
import com.vseonlineshop.payment.webhook.exception.WebhookProcessingException;
import com.vseonlineshop.payment.webhook.exception.WebhookSignatureException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

import static com.vseonlineshop.payment.config.LogUtils.safeContext;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@AllArgsConstructor
@Slf4j
public class StripeWebhookHandler implements WebhookHandler {

    private final WebhookVerifier webhookVerifier;
    private final WebhookSupport webhookSupport;
    private final KafkaEventPublisher kafkaEventPublisher;


    @Override
    public boolean supports(String eventSource) {
        return "STRIPE".equalsIgnoreCase(eventSource);
    }

    @Override
    public void handle(String payload, Map<String, String> headers) {
        long start = System.currentTimeMillis();
        boolean processingSuccessful = false;

        MDC.put(
                "correlationId",
                "pending"
        );

        try {
            // 1. Signatur prüfen
            Event event;
            try {
                log.info(
                        "Stripe Webhook Verarbeitung gestartet",
                        kv("service", "billing-service"),
                        kv("context", safeContext(
                                "provider", "stripe"
                        ))
                );
                event = webhookVerifier.verifyStripe(
                        payload, headers.get("Stripe-Signature")
                );
            } catch (IllegalArgumentException | SignatureVerificationException e) {
                log.warn(
                        "Stripe Webhook Signatur ungültig",
                        kv("service", "billing-service"),
                        kv("context", safeContext(
                                "provider", "stripe",
                                "errorType", e.getClass().getSimpleName()
                        ))
                );
                throw new WebhookSignatureException("Ungültige Stripe-Signatur: " + e.getMessage());
            }

            StripeObject stripeObject;

            try {
                stripeObject = event
                        .getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() ->
                                new WebhookProcessingException("Deserialization fehlgeschlagen")
                        );

            } catch (WebhookProcessingException e) {

                log.error(
                        "Stripe Webhook Deserialization fehlgeschlagen",
                        kv("service", "billing-service"),
                        kv("context", safeContext(
                                "provider", "stripe",
                                "eventType", event.getType()
                        )),
                        e
                );

                throw e;
            }


            PaymentIntent paymentIntent = webhookSupport.getPaymentIntent(event, stripeObject);
            if (paymentIntent == null) {
                log.info(
                        "Stripe Webhook Event ignoriert",
                        kv("service", "billing-service"),
                        kv("context", safeContext(
                                "provider", "stripe",
                                "eventType", event.getType()
                        ))
                );

                return;
            }

            // Metadaten & Extrahierung
            WebHookMetadata metadata = webhookSupport.extractMetadata(paymentIntent);

            MDC.put(
                    "correlationId",
                    metadata.correlationId() != null
                            ? metadata.correlationId()
                            : "unknown"
            );

            log.debug(
                    "Stripe Webhook Metadaten extrahiert",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "stripe",
                            "paymentId", metadata.paymentIdRaw(),
                            "orderId", metadata.orderId()
                    ))
            );

            String checkoutSessionId = webhookSupport.extractCheckoutSessionId(paymentIntent);


            log.info(
                    "Stripe Webhook empfangen",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "stripe",
                            "eventType", event.getType(),
                            "eventId", event.getId(),
                            "paymentId", metadata.paymentIdRaw(),
                            "orderId", metadata.orderId(),
                            "providerTransactionId", checkoutSessionId
                    ))
            );

            // Validierung der Pflichtfelder
            webhookSupport.validateRequiredFields(metadata, checkoutSessionId);

            log.debug(
                    "Stripe Webhook Validierung erfolgreich",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "stripe",
                            "eventId", event.getId(),
                            "paymentId", metadata.paymentIdRaw()
                    ))
            );

            // Laden und Konsistenzprüfung des Payments
            Payment payment = webhookSupport.loadAndVerifyPayment(metadata);

            log.debug(
                    "Payment für Webhook geladen",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "paymentId", payment.getPaymentId(),
                            "eventId", event.getId(),
                            "currentStatus", payment.getStatus()
                    ))
            );

            // Idempotenz-Prüfung
            if (payment.getStatus() == PaymentStatus.CONFIRMED || payment.getStatus() == PaymentStatus.FAILED) {
                log.warn(
                        "Webhook bereits verarbeitet (Idempotenzprüfung)",
                        kv("service", "billing-service"),
                        kv("context", safeContext(
                                "provider", "stripe",
                                "eventId", event.getId(),
                                "paymentId", metadata.paymentIdRaw(),
                                "status", payment.getStatus()
                        ))
                );
                webhookSupport.safePublish(() -> kafkaEventPublisher.publishWebhookDuplicateIgnored(payment));
                return;
            }

            String rawResponse = webhookSupport.toJson(event);

            // Routing des Event-Typs
            switch (event.getType()) {
                case "payment_intent.succeeded" -> {

                    webhookSupport.handleSuccess(payment, checkoutSessionId, null, rawResponse, Instant.now());
                    processingSuccessful = true;
                    log.info(
                            "Stripe Zahlung erfolgreich verarbeitet",
                            kv("service", "billing-service"),
                            kv("context", safeContext(
                                    "provider", "stripe",
                                    "paymentId", payment.getPaymentId(),
                                    "transactionId", checkoutSessionId,
                                    "status", "CONFIRMED"
                            ))
                    );

                }
                case "payment_intent.payment_failed" -> {

                    String errorCode = null;
                    String errorMessage = null;

                    if(paymentIntent.getLastPaymentError()!=null){
                        errorCode = paymentIntent
                                .getLastPaymentError()
                                .getCode();

                        errorMessage =
                                paymentIntent
                                        .getLastPaymentError()
                                        .getMessage();
                    }

                    log.warn(
                            "Stripe Zahlung fehlgeschlagen",
                            kv("service","billing-service"),
                            kv("context",safeContext(
                                    "provider","stripe",
                                    "paymentId",payment.getPaymentId(),
                                    "errorCode",errorCode,
                                    "errorMessage",errorMessage
                            ))
                    );
                    processingSuccessful = true;
                    webhookSupport.handleFailure(payment, checkoutSessionId, errorMessage, rawResponse, Instant.now());
                }
                default -> {

                    log.info(
                            "Stripe Informations-Event verarbeitet",
                            kv("service", "billing-service"),
                            kv("context", safeContext(
                                    "provider", "stripe",
                                    "eventType", event.getType(),
                                    "paymentId", payment.getPaymentId()
                            ))
                    );
                    webhookSupport.handleInformativeEvent(payment, checkoutSessionId, null, event.getType(), rawResponse);
                }
            }


        } catch (WebhookSignatureException | WebhookProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Unerwarteter Fehler bei der Stripe Webhook Verarbeitung",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "stripe",
                            "errorType", e.getClass().getSimpleName(),
                            "errorMessage", e.getMessage()
                    )),
                    e
            );
            webhookSupport.systemAlarm();
            throw new WebhookProcessingException("Interner Fehler: " + e.getMessage());
        } finally {

            log.debug(
                    "Stripe Webhook Verarbeitung beendet",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "stripe",
                            "success", processingSuccessful,
                            "durationMs", System.currentTimeMillis() - start
                    ))
            );

            MDC.clear();
        }
    }
}
