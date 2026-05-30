package com.vseonlineshop.shopservice.dto.payment;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.vseonlineshop.shopservice.domain.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


public record PaymentRequest(
        @JsonProperty("correlationId")    UUID correlationId,
        @JsonProperty("orderId")          UUID orderId,
        @JsonProperty("userId")          UUID userId,
        @JsonProperty("totalAmount")      BigDecimal totalAmount,
        @JsonProperty("currency")         String currency,
        @JsonProperty("paymentMethod")  PaymentMethod paymentMethod, // Unser Enum!
        List<CartItemDTO> cartItems
) {}
