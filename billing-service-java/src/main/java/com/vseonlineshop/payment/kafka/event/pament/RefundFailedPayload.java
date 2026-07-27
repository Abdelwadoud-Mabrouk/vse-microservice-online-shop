package com.vseonlineshop.payment.kafka.event.pament;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;
public record RefundFailedPayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String currency,
        String failureReason,
        String rawResponse
) {}
