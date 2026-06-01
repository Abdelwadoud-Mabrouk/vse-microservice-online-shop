 package com.vseonlineshop.payment.event.payment;


import com.vseonlineshop.payment.dto.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFailedPayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        UUID userId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String currency,
        String failureReason,
        String providerTransactionId,
        String rowResponse
) {
}