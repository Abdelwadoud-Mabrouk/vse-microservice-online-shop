package com.vseonlineshop.shopservice.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDTO(
        String productName,
        long quantity,
         UUID productId,
        BigDecimal pricePerUnit) {
}
