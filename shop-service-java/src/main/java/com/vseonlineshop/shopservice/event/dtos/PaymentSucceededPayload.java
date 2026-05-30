package com.vseonlineshop.shopservice.event.dtos;



import com.vseonlineshop.shopservice.dto.payment.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSucceededPayload(
    UUID paymentId,
    UUID orderId,
    UUID correlationId,
    UUID userId,
    PaymentMethod paymentMethod,
    BigDecimal amount,
    String currency,
    String providerTransactionId,
    String rowResponse
) {}
