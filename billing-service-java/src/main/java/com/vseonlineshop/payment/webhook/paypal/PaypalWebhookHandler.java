package com.vseonlineshop.payment.webhook.paypal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vseonlineshop.payment.dto.webhook.WebHookMetadata;
import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentStatus;
import com.vseonlineshop.payment.exception.GlobalException;
import com.vseonlineshop.payment.kafka.services.KafkaEventPublisher;
import com.vseonlineshop.payment.webhook.tool.WebhookSupport;
import com.vseonlineshop.payment.webhook.WebhookHandler;
import com.vseonlineshop.payment.webhook.exception.WebhookProcessingException;
import com.vseonlineshop.payment.webhook.exception.WebhookSignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

import static com.vseonlineshop.payment.config.LogUtils.safeContext;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Log4j2
public class PaypalWebhookHandler implements WebhookHandler {


    private final WebhookSupport webhookSupport;
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

        long start = System.currentTimeMillis();
        boolean processingSuccessful = false;

        MDC.put(
                "correlationId",
                "pending"
        );
        try {

            log.info(
                    "Paypal Webhook Verarbeitung gestartet",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "Paypal"
                    ))
            );

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
                log.warn(
                        "Paypal Webhook Signatur ungültig",
                        kv("service", "billing-service"),
                        kv("context", safeContext(
                                "provider", "paypal",
                                "verificationStatus", "FAILED"
                        ))
                );
                throw new WebhookSignatureException("Ungültige Signatur");
            }

            log.info(
                    "Paypal Webhook Signatur erfolgreich verifiziert",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "paypal",
                            "transmissionId", transmissionId,
                            "verificationStatus", "SUCCESS"
                    ))
            );

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
                log.error(
                        "Paypal Webhook JSON Parsing fehlgeschlagen",
                        kv("service", "billing-service"),
                        kv("context", safeContext(
                                "provider", "paypal"
                        )),
                        e
                );

                throw new WebhookProcessingException(
                        "Paypal Payload konnte nicht gelesen werden"
                );
            }

            String checkoutSessionId = "UNKNOWN";
            if (resource.has("supplementary_data") &&
                    resource.get("supplementary_data").has("related_ids") &&
                    resource.get("supplementary_data").get("related_ids").has("order_id")) {
                checkoutSessionId = resource.get("supplementary_data").get("related_ids").get("order_id").asText();
            }


            String captureId = resource.has("id") ? resource.get("id").asText() : checkoutSessionId;

            log.info(
                    "Paypal Transaktionsdaten extrahiert",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "paypal",
                            "providerTransactionId", checkoutSessionId,
                            "captureId", captureId
                    ))
            );
            // Die verkettete custom_id aus dem JSON holen und in einheitliches Metadata-Objekt mappen
            String customIdRaw = resource.has("custom_id") ? resource.get("custom_id").asText() : "";
            WebHookMetadata metadata = webhookSupport.getMetadata(customIdRaw, eventType, checkoutSessionId);
            MDC.put(
                    "correlationId",
                    metadata.correlationId() != null
                            ? metadata.correlationId()
                            : "unknown"
            );

            log.debug(
                    "Paypal Webhook Metadaten extrahiert",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "Paypal",
                            "paymentId", metadata.paymentIdRaw(),
                            "orderId", metadata.orderId()
                    ))
            );

            webhookSupport.validateRequiredFields(metadata, checkoutSessionId);


            // Laden und Konsistenzprüfung des Payments gegen die DB
            Payment payment = webhookSupport.loadAndVerifyPayment(metadata);

            log.debug(
                    "Paypal Payment erfolgreich validiert",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "paypal",
                            "paymentId", payment.getPaymentId(),
                            "orderId", payment.getOrderId()
                    ))
            );


            // Idempotenz-Prüfung
            if (payment.getStatus() == PaymentStatus.CONFIRMED || payment.getStatus() == PaymentStatus.FAILED) {
                log.warn(
                        "Paypal bereits verarbeitet (Idempotenzprüfung)",
                        kv("service", "billing-service"),
                        kv("context", safeContext(
                                "provider", "Paypal",
                                "paymentId", metadata.paymentIdRaw(),
                                "status", payment.getStatus()
                        ))
                );
                webhookSupport.safePublish(() -> kafkaEventPublisher.publishWebhookDuplicateIgnored(payment));
                return;
            }

            // Routing des Event-Typs
            switch (eventType) {
                case "PAYMENT.CAPTURE.COMPLETED" -> {
                    webhookSupport.handleSuccess(payment, checkoutSessionId, captureId, payload, Instant.now());
                    processingSuccessful = true;
                    log.info(
                            "Paypal Zahlung erfolgreich verarbeitet",
                            kv("service", "billing-service"),
                            kv("context", safeContext(
                                    "provider", "Paypal",
                                    "paymentId", payment.getPaymentId(),
                                    "transactionId", checkoutSessionId,
                                    "status", "CONFIRMED"
                            ))
                    );
                }
                case "PAYMENT.CAPTURE.DENIED" -> {
                    String dynamischerFehlergrund = "PayPal-Zahlung abgelehnt";
                    if (resource.has("status_details") && resource.get("status_details").has("reason")) {
                        dynamischerFehlergrund = resource.get("status_details").get("reason").asText();
                    }
                    webhookSupport.handleFailure(payment, checkoutSessionId, dynamischerFehlergrund, payload, Instant.now());
                    processingSuccessful = true;
                    log.warn(
                            "Paypal Zahlung fehlgeschlagen",
                            kv("service", "billing-service"),
                            kv("context", safeContext(
                                    "provider", "paypal",
                                    "paymentId", payment.getPaymentId(),
                                    "errorMessage", dynamischerFehlergrund
                            ))
                    );
                }
                default -> {
                    log.info(
                            "Paypal Informations-Event verarbeitet",
                            kv("service", "billing-service"),
                            kv("context", safeContext(
                                    "provider", "Paypal",
                                    "paymentId", payment.getPaymentId()
                            ))
                    );
                    webhookSupport.handleInformativeEvent(payment, checkoutSessionId, null, eventType, payload);
                }
            }


        } catch (WebhookSignatureException | WebhookProcessingException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "Unerwarteter Fehler bei der Paypal Webhook Verarbeitung",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "Paypal",
                            "errorType", e.getClass().getSimpleName(),
                            "errorMessage", e.getMessage()
                    )),
                    e
            );
            webhookSupport.systemAlarm();
            throw new GlobalException("Internal Server Error");
        } finally {
            log.debug(
                    "Paypal Webhook Verarbeitung beendet",
                    kv("service", "billing-service"),
                    kv("context", safeContext(
                            "provider", "Paypal",
                            "success", processingSuccessful,
                            "durationMs", System.currentTimeMillis() - start
                    ))
            );
            MDC.clear();
        }
    }
}
