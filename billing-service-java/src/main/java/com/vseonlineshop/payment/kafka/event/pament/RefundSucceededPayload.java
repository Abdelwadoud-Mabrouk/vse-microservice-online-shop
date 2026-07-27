package com.vseonlineshop.payment.kafka.event.pament;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundSucceededPayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        UUID userId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String currency,
        String providerRefundTransactionId, // Die von Stripe/PayPal erzeugte Refund-ID
        String rawResponse
) {}