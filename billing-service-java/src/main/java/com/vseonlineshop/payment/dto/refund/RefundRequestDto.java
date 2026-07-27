package com.vseonlineshop.payment.dto.refund;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;

import java.math.BigDecimal;


public record RefundRequestDto(
        PaymentMethod method,
        String transactionId,
        BigDecimal amount,
        String currency
) {}