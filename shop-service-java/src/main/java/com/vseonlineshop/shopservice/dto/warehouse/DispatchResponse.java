package com.vseonlineshop.shopservice.dto.warehouse;

import java.util.UUID;

public record DispatchResponse(UUID orderId, UUID correlationId, String status) {
}
