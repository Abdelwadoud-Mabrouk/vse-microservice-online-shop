package com.vseonlineshop.shopservice.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ReservationResponse(
    @JsonProperty("reservationId") UUID reservationId,
    @JsonProperty("order_id")      UUID orderId,
    @JsonProperty("items")         List<ReservationResponseItem> items
) {}