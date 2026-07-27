package com.vseonlineshop.shopservice.product.repository;


import com.vseonlineshop.shopservice.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlugAndActiveTrue(String slug);

    Page<Product> findAllByActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdInAndActiveTrue(List<UUID> categoryIds, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String queryName, String query, Pageable pageable);

    Page<Product> findByColorAndActiveTrue(
            String color,
            Pageable pageable
    );


    Page<Product> findByMaterialAndActiveTrue(
            String material,
            Pageable pageable
    );


    Page<Product> findByPriceBetweenAndActiveTrue(
            BigDecimal min,
            BigDecimal max,
            Pageable pageable
    );

}
