package com.vseonlineshop.shopservice.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequestDto(
        PaymentMethod method,
        BigDecimal amount,
        String currency
) {}