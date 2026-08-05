package com.vseonlineshop.shopservice.product.repository;


import com.vseonlineshop.shopservice.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @EntityGraph(attributePaths = {
            "category",
            "imageUrls"
    })
    Optional<Product> findBySlugAndActiveTrue(String slug);

    @EntityGraph(attributePaths = {
            "category",
            "imageUrls"
    })
    Page<Product> findAllByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "imageUrls"
    })
    Page<Product> findByCategoryIdInAndActiveTrue(List<UUID> categoryIds, Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "imageUrls"
    })
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String queryName, String query, Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "imageUrls"
    })
    Page<Product> findByColorAndActiveTrue(
            String color,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "category",
            "imageUrls"
    })
    Optional<Product> findWithCategoryById(UUID id);

    @EntityGraph(attributePaths = {
            "category",
            "imageUrls"
    })
    Page<Product> findByMaterialAndActiveTrue(
            String material,
            Pageable pageable
    );


    @EntityGraph(attributePaths = {
            "category",
            "imageUrls"
    })
    Page<Product> findByPriceBetweenAndActiveTrue(
            BigDecimal min,
            BigDecimal max,
            Pageable pageable
    );

}
