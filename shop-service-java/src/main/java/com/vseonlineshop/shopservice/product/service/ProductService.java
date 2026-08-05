package com.vseonlineshop.shopservice.product.service;


import com.vseonlineshop.shopservice.product.domain.Category;
import com.vseonlineshop.shopservice.product.domain.Product;
import com.vseonlineshop.shopservice.product.dto.ProductFilter;
import com.vseonlineshop.shopservice.product.dto.ProductResponse;
import com.vseonlineshop.shopservice.product.exception.ResourceNotFoundException;
import com.vseonlineshop.shopservice.product.repository.CategoryRepository;
import com.vseonlineshop.shopservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;


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


        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Kategorie mit Slug " + categorySlug + " nicht gefunden."
                        )
                );


        List<UUID> categoryIds = new ArrayList<>();

        collectCategoryIds(category, categoryIds);


        return productRepository
                .findByCategoryIdInAndActiveTrue(categoryIds, pageable)
                .map(this::mapToResponse);
    }

    private void collectCategoryIds(
            Category category,
            List<UUID> ids
    ) {

        ids.add(category.getId());

        if (!category.getSubCategories().isEmpty()){

            for (Category child : category.getSubCategories()) {

                collectCategoryIds(child, ids);

            }
        }
    }


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



    public Page<ProductResponse> filterProducts(
            ProductFilter filter,
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        throw new UnsupportedOperationException(
                "Combined filter not implemented yet."
        );
    }


    public ProductResponse getProductById(UUID id) {

        Product product = productRepository.findWithCategoryById(id)
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
                .categoryName(
                        product.getCategory() != null
                                ? product.getCategory().getName()
                                : null
                )
                .categorySlug(
                        product.getCategory() != null
                                ? product.getCategory().getSlug()
                                : null
                )
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .imageUrls(
                        product.getImageUrls() != null
                                ? new ArrayList<>(product.getImageUrls())
                                : List.of()
                )
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
