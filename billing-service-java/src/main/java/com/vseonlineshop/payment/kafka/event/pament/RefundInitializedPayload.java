package com.vseonlineshop.payment.kafka.event.pament;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundInitializedPayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        UUID userId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String currency,
        String status
) {}