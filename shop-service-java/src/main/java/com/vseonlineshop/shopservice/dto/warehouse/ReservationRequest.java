package com.vseonlineshop.shopservice.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ReservationRequest(
    @JsonProperty("orderId")      UUID orderId,
    @JsonProperty("items")         List<ReservationRequestItem> items
) {}