package com.vseonlineshop.payment.dto;


import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentRequest(
        UUID correlationId,
        @NotNull UUID orderId,
        @NotNull @Positive BigDecimal totalAmount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        UUID userId,
        PaymentMethod paymentMethod,
        List<CartItemDTO> cartItems
) {

    public PaymentRequest withCorrelationId(UUID correlationId) {
        return new PaymentRequest(
                correlationId,
                orderId,
                totalAmount,
                currency,
                userId,
                paymentMethod,
                cartItems
        );
    }
}
