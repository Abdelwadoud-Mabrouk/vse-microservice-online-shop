package com.vseonlineshop.payment.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItem {

    private Long id;

    private UUID productId;

    private String productName;

    private Integer quantity;

    private BigDecimal pricePerUnit;

    private BigDecimal subTotal;
}