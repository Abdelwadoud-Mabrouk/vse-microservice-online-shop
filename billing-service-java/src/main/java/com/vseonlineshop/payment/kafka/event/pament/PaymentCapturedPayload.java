package com.vseonlineshop.payment.kafka.event.pament;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCapturedPayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String paypalCaptureId
) {}
