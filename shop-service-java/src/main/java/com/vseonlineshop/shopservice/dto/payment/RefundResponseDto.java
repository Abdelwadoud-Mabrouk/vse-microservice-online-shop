package com.vseonlineshop.shopservice.dto.payment;


public record RefundResponseDto(
        String providerRefundTransactionId,
        String status
) {}
