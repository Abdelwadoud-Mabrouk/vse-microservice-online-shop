package com.vseonlineshop.payment.event.payment;



import com.vseonlineshop.payment.dto.PaymentMethod;
import com.vseonlineshop.payment.enums.PaymentProvider;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSucceededPayload(
    UUID paymentId,
    UUID orderId,
    UUID correlationId,
    UUID userId,
    PaymentMethod paymentProvider,
    BigDecimal amount,
    String currency,
    String providerTransactionId,
    String rowResponse
) {}
