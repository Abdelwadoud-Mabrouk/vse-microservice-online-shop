package com.vseonlineshop.payment.webhook.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.vseonlineshop.payment.dto.WebHookMetadata;
import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.enums.PaymentStatus;
import com.vseonlineshop.payment.service.KafkaEventPublisher;
import com.vseonlineshop.payment.webhook.HelpTool;
import com.vseonlineshop.payment.webhook.WebhookHandler;
import com.vseonlineshop.payment.webhook.exception.WebhookProcessingException;
import com.vseonlineshop.payment.webhook.exception.WebhookSignatureException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class StripeWebhookHandler implements WebhookHandler {

    private final WebhookVerifier webhookVerifier;
    private final HelpTool helpTool;
    private final KafkaEventPublisher kafkaEventPublisher;


    @Override
    public boolean supports(String eventSource) {
        return "STRIPE".equalsIgnoreCase(eventSource);
    }

    @Override
    public void handle(String payload, Map<String, String> headers) {

        try {
            // 1. Signatur prüfen
            Event event;
            try {
                event = webhookVerifier.verifyStripe(
                        payload, headers.get("Stripe-Signature")
                );
            } catch (IllegalArgumentException | SignatureVerificationException e) {
                log.warn("[WEBHOOK/STRIPE] Signatur ungültig: {}", e.getMessage());
                throw new WebhookSignatureException("Ungültige Stripe-Signatur: " + e.getMessage());
            }

            StripeObject stripeObject = event
                    .getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new WebhookProcessingException("Deserialization fehlgeschlagen"));


            PaymentIntent paymentIntent = helpTool.getPaymentIntent(event, stripeObject);
            if (paymentIntent == null) {
                return;
            }

            // Metadaten & Extrahierung
            WebHookMetadata metadata = helpTool.extractMetadata(paymentIntent);
            String checkoutSessionId = helpTool.extractCheckoutSessionId(paymentIntent);

            log.info("[WEBHOOK/STRIPE] event={} providerTxn={} paymentId={} orderId={} correlationId={}",
                    event.getType(), checkoutSessionId, metadata.paymentIdRaw(), metadata.orderId(), metadata.correlationId());

            // Validierung der Pflichtfelder
            helpTool.validateRequiredFields(metadata, checkoutSessionId);

            // Laden und Konsistenzprüfung des Payments
            Payment payment = helpTool.loadAndVerifyPayment(metadata);

            // Idempotenz-Prüfung
            if (payment.getStatus() == PaymentStatus.CONFIRMED || payment.getStatus() == PaymentStatus.FAILED) {
                log.warn("[WEBHOOK/STRIPE] Bereits verarbeitet paymentId={} status={}", metadata.paymentIdRaw(), payment.getStatus());
                helpTool.safePublish(() -> kafkaEventPublisher.publishWebhookDuplicateIgnored(payment));
                return;
            }

            String rawResponse = helpTool.toJson(event);

            // Routing des Event-Typs
            switch (event.getType()) {
                case "payment_intent.succeeded" ->
                        helpTool.handleSuccess(payment, checkoutSessionId, rawResponse, Instant.now());
                case "payment_intent.payment_failed" -> {
                    String dynamischerFehlergrund = "Unbekannter Zahlungsfehler";
                    if (paymentIntent.getLastPaymentError() != null) {
                        var stripeError = paymentIntent.getLastPaymentError();
                        dynamischerFehlergrund = stripeError.getCode() + ": " + stripeError.getMessage();
                    }
                    helpTool.handleFailure(payment, checkoutSessionId, dynamischerFehlergrund, rawResponse, Instant.now());
                }
                default -> helpTool.handleInformativeEvent(payment, checkoutSessionId, event.getType(), rawResponse);
            }


        } catch (WebhookSignatureException | WebhookProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WEBHOOK/STRIPE] Unerwarteter Fehler", e);
            helpTool.systemAlarm();
            throw new WebhookProcessingException("Interner Fehler: " + e.getMessage());
        }
    }
}
