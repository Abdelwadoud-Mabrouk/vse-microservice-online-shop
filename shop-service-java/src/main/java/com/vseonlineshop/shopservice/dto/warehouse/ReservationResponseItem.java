package com.vseonlineshop.shopservice.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;


public record ReservationResponseItem(
    @JsonProperty("productId") UUID productId,
    @JsonProperty("productName") String productName,
    @JsonProperty("quantity")    int quantity
) {}