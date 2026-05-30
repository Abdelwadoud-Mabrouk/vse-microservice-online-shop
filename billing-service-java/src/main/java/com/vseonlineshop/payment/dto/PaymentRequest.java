package com.vseonlineshop.payment.dto;


import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentRequest(
        @NotNull UUID correlationId,
        @NotNull UUID orderId,
        @NotNull @Positive BigDecimal totalAmount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        UUID userId,
        PaymentMethod paymentMethod,
        List<OrderItem> cartItems
) {
}
