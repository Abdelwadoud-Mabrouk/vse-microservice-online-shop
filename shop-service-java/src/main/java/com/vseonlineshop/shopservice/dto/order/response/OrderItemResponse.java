package com.vseonlineshop.shopservice.dto.order.response;


import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal pricePerUnit,
        BigDecimal subTotal,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalPrice
) {
}