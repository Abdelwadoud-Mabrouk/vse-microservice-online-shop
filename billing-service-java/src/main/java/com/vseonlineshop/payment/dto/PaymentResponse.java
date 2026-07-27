package com.vseonlineshop.payment.dto;

import com.vseonlineshop.payment.entity.PaymentStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentResponse(
    UUID correlationId,
    UUID orderId,
    UUID paymentId,
    String approvalUrl,
    PaymentStatus status,
    Instant paidAt
) {}
