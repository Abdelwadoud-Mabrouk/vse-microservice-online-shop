package com.vseonlineshop.shopservice.dto.payment;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.vseonlineshop.shopservice.domain.PaymentProvider;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shop → Billing-Service
 * paymentProvider wird mitgeschickt, damit Billing den richtigen
 * externen Provider (PayPal / Stripe / Klarna) wählen kann.
 */
public record PaymentRequest(
    @JsonProperty("correlationId")    UUID correlationId,
    @JsonProperty("orderId")          UUID orderId,
    @JsonProperty("totalAmount")      BigDecimal totalAmount,
    @JsonProperty("currency")         String currency,
    @JsonProperty("paymentProvider") PaymentProvider paymentProvider
) {}