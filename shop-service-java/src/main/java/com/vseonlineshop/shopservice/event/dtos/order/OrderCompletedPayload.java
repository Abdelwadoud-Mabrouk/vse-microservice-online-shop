package com.vseonlineshop.shopservice.event.dtos.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCompletedPayload(
        UUID orderId,
        UUID userId,
        String paymentId,
        BigDecimal totalAmount,
        String currency,
        String paymentProvider
) {}
