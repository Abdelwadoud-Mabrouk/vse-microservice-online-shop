package com.vseonlineshop.shopservice.event.dtos;



import com.vseonlineshop.shopservice.domain.PaymentProvider;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSucceededPayload(
    UUID paymentId,
    UUID orderId,
    UUID correlationId,
    UUID userId,
    PaymentProvider paymentProvider,
    BigDecimal amount,
    String currency,
    String providerTransactionId,
    String rowResponse
) {}
