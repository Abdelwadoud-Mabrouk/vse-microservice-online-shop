package com.vseonlineshop.shopservice.event;

import java.util.UUID;

public record OrderCancelledPayload(
        UUID orderId,
        UUID userId,
        String reason
) {}
