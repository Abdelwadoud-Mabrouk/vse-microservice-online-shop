package com.vseonlineshop.payment.dto;

import com.vseonlineshop.payment.enums.PaymentProvider;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
    @NotNull UUID correlationId,
    @NotNull UUID orderId,
    @NotNull @Positive BigDecimal totalAmount,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull PaymentProvider paymentProvider,
    @NotNull UUID userId
) {}
