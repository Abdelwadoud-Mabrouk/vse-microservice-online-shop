package com.vseonlineshop.shopservice.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ReservationRequestItem(
    @JsonProperty("productId") UUID productId,
    @JsonProperty("quantity")  int quantity,
    @JsonProperty("sku") String sku
) {}