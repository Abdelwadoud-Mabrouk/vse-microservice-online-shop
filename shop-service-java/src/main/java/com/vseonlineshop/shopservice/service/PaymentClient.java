package com.vseonlineshop.shopservice.service;


import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.payment.PaymentRequest;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


/**
 * Echte Implementierung: RestTemplate / WebClient auf ${payment.base-url}/payments
 * Der paymentProvider aus dem Order-Objekt wird in den Request eingebaut,
 * damit das echte Billing-Service später den richtigen Provider (PayPal/Stripe/Klarna)
 * aufrufen kann.
 */
@Service
@Slf4j
public class PaymentClient {


    private final RestClient restClient;

    public PaymentClient(@Value("${services.payment.url}") String paymentServiceUrl, KafkaEventPublisher kafkaEventPublisher) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
    }

    public PaymentResponse pay(Order order) {
        log.info("[PaymentClient] Synchroner Call für Order={}", order.getId());

        PaymentRequest request = new PaymentRequest(
                order.getCorrelationId(),
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPaymentProvider()
        );

        try {
            return restClient.post()
                    .uri("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PaymentResponse.class);

        } catch (RestClientException ex) {
            log.error("[CRITICAL] Payment-Service ist DOWN für Order={}: {}", order.getId(), ex.getMessage());
            throw ex;
        }
    }
}