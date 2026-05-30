package com.vseonlineshop.payment.event.payment;

import com.vseonlineshop.payment.dto.PaymentMethod;
import com.vseonlineshop.payment.enums.PaymentStatus;

import java.util.UUID;

public record WebhookDuplicatePayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        PaymentMethod paymentMethod,
        PaymentStatus currentStatus
) {}
