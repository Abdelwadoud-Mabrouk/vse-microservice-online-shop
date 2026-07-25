package com.vseonlineshop.shopservice.dto.payment;

import java.util.UUID;

public record RefundResponseDto(
        UUID correlationId,
        String providerRefundTransactionId,
        String status
) {}
