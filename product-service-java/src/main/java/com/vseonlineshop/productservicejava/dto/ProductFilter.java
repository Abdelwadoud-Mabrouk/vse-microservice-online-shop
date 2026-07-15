package com.vseonlineshop.productservicejava.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductFilter {

    private String category;

    private String search;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String color;

    private String material;
}