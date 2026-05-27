package com.vseonlineshop.shopservice.event.dtos;

import java.util.List;
import java.util.UUID;

public record OrderCreatedPayload(
        UUID orderId,
        UUID userId,
        List<UUID> productIds,
        String currency,
        String paymentProvider
) {}