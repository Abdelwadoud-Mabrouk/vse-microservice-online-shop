package com.vseonlineshop.shopservice.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
    @JsonProperty("correlationId") UUID correlationId,
    @JsonProperty("orderId")       UUID orderId,
    @JsonProperty("paymentId")     String paymentId,
    @JsonProperty("status")        String status,
    @JsonProperty("paidAt")        LocalDateTime paidAt
) {}