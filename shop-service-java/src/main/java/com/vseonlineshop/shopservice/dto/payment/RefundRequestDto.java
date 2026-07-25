package com.vseonlineshop.shopservice.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequestDto(
        UUID correlationId,
        PaymentMethod method,
        BigDecimal amount,
        String currency
) {}