package com.vseonlineshop.shopservice.product.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID id;
    private String sku;
    private String name;
    private String slug;
    private String description;

    private String categoryName;
    private String categorySlug;

    private BigDecimal price;
    private BigDecimal originalPrice;
    private List<String> imageUrls;

    private Double widthCm;
    private Double heightCm;
    private Double depthCm;
    private Double weightKg;
    private String material;
    private String color;
    private boolean active;
}