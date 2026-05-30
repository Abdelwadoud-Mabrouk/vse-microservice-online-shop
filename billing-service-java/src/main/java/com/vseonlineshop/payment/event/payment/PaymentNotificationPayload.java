package com.vseonlineshop.payment.event.payment;

import com.vseonlineshop.payment.dto.PaymentMethod;
import com.vseonlineshop.payment.enums.PaymentProvider;

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