package com.vseonlineshop.payment.event.payment;


import com.vseonlineshop.payment.dto.PaymentMethod;
import com.vseonlineshop.payment.enums.PaymentStatus;

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