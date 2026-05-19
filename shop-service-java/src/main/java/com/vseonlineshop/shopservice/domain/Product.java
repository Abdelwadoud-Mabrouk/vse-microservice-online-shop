package com.vseonlineshop.shopservice.domain;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}