package com.vseonlineshop.payment.kafka.event.pament;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;

import java.util.UUID;

public record PaymentNotificationPayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        PaymentMethod paymentProvider,
        String providerEvent,
        String providerTransactionId,
        String rawResponse
) {}