package com.vseonlineshop.payment.kafka.event.pament;


import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.entity.PaymentStatus;

import java.util.UUID;

public record PaymentInitialedPayload(
        UUID paymentId,
        UUID orderId,
        UUID correlationId,
        UUID userId,
        PaymentMethod paymentProvider,
        String currency,
        PaymentStatus paymentStatus

) {
}