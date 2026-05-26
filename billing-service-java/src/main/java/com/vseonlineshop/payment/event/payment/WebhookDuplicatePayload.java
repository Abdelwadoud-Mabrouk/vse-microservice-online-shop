package com.vseonlineshop.payment.event.payment;

import com.vseonlineshop.payment.enums.PaymentProvider;
import com.vseonlineshop.payment.enums.PaymentStatus;

import java.util.UUID;

public record WebhookDuplicatePayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        PaymentProvider paymentProvider,
        PaymentStatus currentStatus
) {}
