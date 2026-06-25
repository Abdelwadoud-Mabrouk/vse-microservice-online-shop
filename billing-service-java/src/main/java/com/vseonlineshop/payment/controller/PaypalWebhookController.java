package com.vseonlineshop.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/payments/paypal")
@RequiredArgsConstructor
@Slf4j
public class PaypalWebhookController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${paypal.webhook.id}")
    private String webhookId;

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    private static final String SANDBOX_BASE = "https://api-m.sandbox.paypal.com";

    @PostMapping("/webhook")
    public ResponseEntity<String> handlePaypalWebhook(
            @RequestHeader("PAYPAL-TRANSMISSION-ID") String transmissionId,
            @RequestHeader("PAYPAL-TRANSMISSION-TIME") String transmissionTime,
            @RequestHeader("PAYPAL-TRANSMISSION-SIG") String signature,
            @RequestHeader("PAYPAL-CERT-URL") String certUrl,
            @RequestHeader("PAYPAL-AUTH-ALGO") String authAlgo,
            @RequestBody String payload) {

        log.info("[PAYPAL/WEBHOOK] Event empfangen. Validiere Signatur...");

        try {
            // 1. Eigenes Sandbox-Access-Token holen (unabhängig vom SDK, garantiert funktionsfähig)
            String accessToken = fetchAccessToken();

            // 2. Verifizierungs-Body bauen
            Map<String, Object> verificationRequest = new HashMap<>();
            verificationRequest.put("auth_algo", authAlgo);
            verificationRequest.put("cert_url", certUrl);
            verificationRequest.put("transmission_id", transmissionId);
            verificationRequest.put("transmission_time", transmissionTime);
            verificationRequest.put("transmission_sig", signature);
            verificationRequest.put("webhook_id", webhookId);
            verificationRequest.put("webhook_event", objectMapper.readTree(payload));

            // 3. Verify-Call an PayPal
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(verificationRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    SANDBOX_BASE + "/v1/notifications/verify-webhook-signature",
                    entity,
                    JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            if (responseBody == null
                    || !"SUCCESS".equalsIgnoreCase(responseBody.get("verification_status").asText())) {
                log.warn("[PAYPAL/WEBHOOK] Signaturprüfung FEHLGESCHLAGEN!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ungültige Signatur");
            }

            log.info("[PAYPAL/WEBHOOK] Signatur ERFOLGREICH verifiziert!");
            return ResponseEntity.ok("Webhook verarbeitet (nur Header-Check, kein DB-Update im Test)");

        } catch (Exception e) {
            log.error("[PAYPAL/WEBHOOK-ERROR] Fehler bei der Verarbeitung", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String fetchAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                SANDBOX_BASE + "/v1/oauth2/token",
                request,
                JsonNode.class
        );

        return response.getBody().get("access_token").asText();
    }
}