package com.vseonlineshop.payment.kafka.event.pament;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.entity.PaymentStatus;

import java.util.UUID;

public record WebhookDuplicatePayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        PaymentMethod paymentMethod,
        PaymentStatus currentStatus
) {}
