package com.vseonlineshop.payment.webhook.paypal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vseonlineshop.payment.dto.webhook.WebHookMetadata;
import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentStatus;
import com.vseonlineshop.payment.exception.GlobalException;
import com.vseonlineshop.payment.kafka.services.KafkaEventPublisher;
import com.vseonlineshop.payment.webhook.HelpTool;
import com.vseonlineshop.payment.webhook.WebhookHandler;
import com.vseonlineshop.payment.webhook.exception.WebhookProcessingException;
import com.vseonlineshop.payment.webhook.exception.WebhookSignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class PaypalWebhookHandler implements WebhookHandler {


    private final HelpTool helpTool;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final PaypalTokenService paypalTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();


    @Value("${paypal.webhook.id}")
    private String webhookId;

    @Value("${sandbox_base}")
    private String sandboxBase;

    @Override
    public boolean supports(String eventSource) {
        return "PAYPAL".equalsIgnoreCase(eventSource);
    }

    @Override
    public void handle(String payload, Map<String, String> mapHeaders) {
        try {
            String accessToken = paypalTokenService.getValidAccessToken();

            // 2. Verifizierungs-Body bauen
            String authAlgo = mapHeaders.get("PAYPAL-AUTH-ALGO");
            String certUrl = mapHeaders.get("PAYPAL-CERT-URL");
            String transmissionId = mapHeaders.get("PAYPAL-TRANSMISSION-ID");
            String transmissionTime = mapHeaders.get("PAYPAL-TRANSMISSION-TIME");
            String signature = mapHeaders.get("PAYPAL-TRANSMISSION-SIG");

            String verificationRequestBody = String.format(
                    "{" +
                            "\"auth_algo\":\"%s\"," +
                            "\"cert_url\":\"%s\"," +
                            "\"transmission_id\":\"%s\"," +
                            "\"transmission_time\":\"%s\"," +
                            "\"transmission_sig\":\"%s\"," +
                            "\"webhook_id\":\"%s\"," +
                            "\"webhook_event\":%s" +
                            "}",
                    authAlgo, certUrl, transmissionId, transmissionTime, signature, webhookId, payload
            );

            // 3. Verify-Call an PayPal
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);


            HttpEntity<String> entity = new HttpEntity<>(verificationRequestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    sandboxBase + "/v1/notifications/verify-webhook-signature",
                    entity,
                    String.class
            );

            JsonNode responseBody = objectMapper.readTree(response.getBody());
            if (responseBody == null || !"SUCCESS".equalsIgnoreCase(responseBody.get("verification_status").asText())) {
                log.warn("[PAYPAL/WEBHOOK] Signaturprüfung FEHLGESCHLAGEN!");
                throw new WebhookSignatureException("Ungültige Signatur");
            }

            log.info("[PAYPAL/WEBHOOK] Signatur ERFOLGREICH verifiziert!");

            JsonNode webhookEvent;
            String eventType;
            JsonNode resource;

            try {
                webhookEvent = objectMapper.readTree(payload);
                if (webhookEvent != null) {
                    eventType = webhookEvent.get("event_type").asText();
                    resource = webhookEvent.get("resource");
                } else {
                    throw new WebhookProcessingException("Fehler beim JSON der Verifizierung");
                }

            } catch (JsonProcessingException e) {
                throw new WebhookProcessingException("Fehler beim JSON der Verifizierung");
            }

            String checkoutSessionId = "UNKNOWN";
            if (resource.has("supplementary_data") &&
                    resource.get("supplementary_data").has("related_ids") &&
                    resource.get("supplementary_data").get("related_ids").has("order_id")) {
                checkoutSessionId = resource.get("supplementary_data").get("related_ids").get("order_id").asText();
            }


            String captureId = resource.has("id") ? resource.get("id").asText() : checkoutSessionId;

            log.info("[PAYPAL/WEBHOOK] Empfangene IDs -> Order-ID: {}, Capture-ID: {}", checkoutSessionId, captureId);
            // Die verkettete custom_id aus dem JSON holen und in einheitliches Metadata-Objekt mappen
            String customIdRaw = resource.has("custom_id") ? resource.get("custom_id").asText() : "";
            WebHookMetadata metadata = helpTool.getMetadata(customIdRaw, eventType, checkoutSessionId);

            // Validierung der Pflichtfelder
            helpTool.validateRequiredFields(metadata, checkoutSessionId);

            // Laden und Konsistenzprüfung des Payments gegen die DB
            Payment payment = helpTool.loadAndVerifyPayment(metadata);

            // Idempotenz-Prüfung
            if (payment.getStatus() == PaymentStatus.CONFIRMED || payment.getStatus() == PaymentStatus.FAILED) {
                log.warn("[WEBHOOK/PAYPAL] Bereits verarbeitet paymentId={} status={}", metadata.paymentIdRaw(), payment.getStatus());
                helpTool.safePublish(() -> kafkaEventPublisher.publishWebhookDuplicateIgnored(payment));
                return;
            }

            // Routing des Event-Typs
            switch (eventType) {
                case "PAYMENT.CAPTURE.COMPLETED" ->
                        helpTool.handleSuccess(payment, checkoutSessionId,captureId, payload, Instant.now());
                case "PAYMENT.CAPTURE.DENIED" -> {
                    String dynamischerFehlergrund = "PayPal-Zahlung abgelehnt";
                    if (resource.has("status_details") && resource.get("status_details").has("reason")) {
                        dynamischerFehlergrund = resource.get("status_details").get("reason").asText();
                    }
                    helpTool.handleFailure(payment, checkoutSessionId, dynamischerFehlergrund, payload, Instant.now());
                }
                default -> helpTool.handleInformativeEvent(payment, checkoutSessionId,null, eventType, payload);
            }


        } catch (WebhookSignatureException | WebhookProcessingException e) {
            throw e;

        } catch (Exception e) {
            log.error("[WEBHOOK/STRIPE] Unerwarteter Fehler", e);
            helpTool.systemAlarm();
            throw new GlobalException("Internal Server Error");
        }
    }
}
