package com.vseonlineshop.payment.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CartItemDTO {

    private Long id;

    private UUID productId;

    private String productName;

    private Integer quantity;

    private BigDecimal totalPrice;
}