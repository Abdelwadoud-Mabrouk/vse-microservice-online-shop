package com.vseonlineshop.productservicejava.service;

import com.vseonlineshop.productservicejava.domain.Category;
import com.vseonlineshop.productservicejava.domain.Product;
import com.vseonlineshop.productservicejava.dto.ProductFilter;
import com.vseonlineshop.productservicejava.dto.ProductResponse;
import com.vseonlineshop.productservicejava.exception.ResourceNotFoundException;
import com.vseonlineshop.productservicejava.repository.CategoryRepository;
import com.vseonlineshop.productservicejava.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;


    // ==========================================================
    // Landing Page
    // ==========================================================

    public Page<ProductResponse> getAllActiveProducts(
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        return productRepository
                .findAllByActiveTrue(pageable)
                .map(this::mapToResponse);
    }


    // ==========================================================
    // Kategorie
    // ==========================================================

    public Page<ProductResponse> getProductsByCategory(
            String categorySlug,
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortBy)
        );


        // 1. Kategorie über den Slug finden
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Kategorie mit Slug " + categorySlug + " nicht gefunden."
                        )
                );


        // 2. Alle Kategorien sammeln
        List<UUID> categoryIds = new ArrayList<>();

        collectCategoryIds(category, categoryIds);


        // 3. Produkte aus allen Kategorien laden
        return productRepository
                .findByCategoryIdInAndActiveTrue(categoryIds, pageable)
                .map(this::mapToResponse);
    }

    private void collectCategoryIds(
            Category category,
            List<UUID> ids
    ) {

        ids.add(category.getId());

        if (category.getSubCategories() != null) {

            for (Category child : category.getSubCategories()) {

                collectCategoryIds(child, ids);

            }
        }
    }
    // ==========================================================
    // Suche
    // ==========================================================

    public Page<ProductResponse> searchProducts(
            String query,
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        return productRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        query,
                        query,
                        pageable
                )
                .map(this::mapToResponse);
    }


    // ==========================================================
    // Farbe
    // ==========================================================

    public Page<ProductResponse> getProductsByColor(
            String color,
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        return productRepository
                .findByColorAndActiveTrue(color, pageable)
                .map(this::mapToResponse);
    }


    // ==========================================================
    // Material
    // ==========================================================

    public Page<ProductResponse> getProductsByMaterial(
            String material,
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        return productRepository
                .findByMaterialAndActiveTrue(material, pageable)
                .map(this::mapToResponse);
    }


    // ==========================================================
    // Preisbereich
    // ==========================================================

    public Page<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        return productRepository
                .findByPriceBetweenAndActiveTrue(
                        minPrice,
                        maxPrice,
                        pageable
                )
                .map(this::mapToResponse);
    }


    // ==========================================================
    // Kombination mehrerer Filter (Specification)
    // ==========================================================

    public Page<ProductResponse> filterProducts(
            ProductFilter filter,
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        // TODO:
        // ProductSpecification erstellen
        // return productRepository.findAll(specification, pageable)
        //        .map(this::mapToResponse);

        throw new UnsupportedOperationException(
                "Combined filter not implemented yet."
        );
    }


    // ==========================================================
    // Produktdetails
    // ==========================================================

    public ProductResponse getProductById(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Produkt mit ID " + id + " nicht gefunden."
                        ));

        return mapToResponse(product);
    }


    public ProductResponse getProductBySlug(String slug) {

        Product product = productRepository
                .findBySlugAndActiveTrue(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Produkt mit Slug " + slug + " nicht gefunden."
                        ));

        return mapToResponse(product);
    }

    //  Alle Produkte (für Admin-Bereich)
    public Page<ProductResponse> getAllProducts(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return productRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categorySlug(product.getCategory() != null ? product.getCategory().getSlug() : null)
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .imageUrls(product.getImageUrls())
                .widthCm(product.getWidthCm())
                .heightCm(product.getHeightCm())
                .depthCm(product.getDepthCm())
                .weightKg(product.getWeightKg())
                .material(product.getMaterial())
                .color(product.getColor())
                .active(product.isActive())
                .build();
    }
}
