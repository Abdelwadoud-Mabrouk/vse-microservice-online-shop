package com.vseonlineshop.shopservice.dto.payment;


import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID orderId,
        UUID paymentId,
        String approvalUrl,
        PaymentStatus status,
        Instant paidAt
) {}