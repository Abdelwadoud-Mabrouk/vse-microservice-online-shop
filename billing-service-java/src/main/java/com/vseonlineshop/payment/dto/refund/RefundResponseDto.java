package com.vseonlineshop.payment.dto.refund;

import java.util.UUID;

public record RefundResponseDto(
        UUID correlationId,
        String providerRefundTransactionId,
        String status
) {}
