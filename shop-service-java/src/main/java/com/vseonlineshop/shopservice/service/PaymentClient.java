package com.vseonlineshop.shopservice.service;


import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.dto.payment.PaymentRequest;
import com.vseonlineshop.shopservice.dto.payment.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Echte Implementierung: RestTemplate / WebClient auf ${payment.base-url}/payments
 * Der paymentProvider aus dem Order-Objekt wird in den Request eingebaut,
 * damit das echte Billing-Service später den richtigen Provider (PayPal/Stripe/Klarna)
 * aufrufen kann.
 */
@Service
@Slf4j
public class PaymentClient {

    /**
     * Sendet POST /payments an Billing-Service.
     * <p>
     * Verhalten: gibt immer CONFIRMED zurück.
     * Werde gegen echten HTTP-Call ausgetauscht, sobald Billing deployed.
     */
    public PaymentResponse pay(Order order) {
        log.info("[Payment] Zahlung für Order {} via {} (Betrag: {} {})",
                order.getId(), order.getPaymentProvider(),
                order.getTotalAmount(), order.getCurrency());

        PaymentRequest request = new PaymentRequest(
                order.getCorrelationId(),
                order.getId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPaymentProvider()
        );
        log.debug("[Payment] Request: {}", request);

        // Response: immer CONFIRMED
        PaymentResponse response = new PaymentResponse(
                order.getCorrelationId(),
                order.getId(),
                UUID.randomUUID().toString(),
                "CONFIRMED",
                LocalDateTime.now()
        );

        log.info("[Payment-Mock] Zahlung CONFIRMED, paymentId={}", response.paymentId());
        return response;
    }
}