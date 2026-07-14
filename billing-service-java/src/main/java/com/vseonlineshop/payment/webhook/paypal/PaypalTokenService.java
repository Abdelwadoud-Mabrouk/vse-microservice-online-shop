package com.vseonlineshop.payment.webhook.paypal;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.time.Instant;

@Service
@Slf4j
public class PaypalTokenService {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${sandbox_base}")
    private String sandboxBase;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedToken = null;
    private Instant tokenExpiry = Instant.MIN;

    public synchronized String getValidAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return cachedToken;
        }

        log.info("[PAYPAL] Hole neues Access Token...");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                sandboxBase + "/v1/oauth2/token",
                request,
                String.class
        );

        try {
            JsonNode responseBody = objectMapper.readTree(response.getBody());

            if (responseBody == null) {
                throw new IllegalStateException("Keine Antwort von PayPal erhalten.");
            }

            this.cachedToken = responseBody.get("access_token").asText();
            long expiresInSeconds = responseBody.has("expires_in") ? responseBody.get("expires_in").asLong() : 32400;
            this.tokenExpiry = Instant.now().plusSeconds(expiresInSeconds);

            return this.cachedToken;

        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Parsen des PayPal-Tokens", e);
        }
    }
}
