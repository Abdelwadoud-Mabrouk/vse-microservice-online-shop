package com.vseonlineshop.shopservice.product.controller;


import com.vseonlineshop.shopservice.product.dto.ProductFilter;
import com.vseonlineshop.shopservice.product.dto.ProductResponse;
import com.vseonlineshop.shopservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;


    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {

        return ResponseEntity.ok(
                productService.getAllActiveProducts(page, size, sortBy)
        );
    }


    @GetMapping("/category/{slug}")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {

        return ResponseEntity.ok(
                productService.getProductsByCategory(slug, page, size, sortBy)
        );
    }


    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {

        return ResponseEntity.ok(
                productService.searchProducts(query, page, size, sortBy)
        );
    }

    @GetMapping("/color/{color}")
    public ResponseEntity<Page<ProductResponse>> getProductsByColor(
            @PathVariable String color,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {

        return ResponseEntity.ok(
                productService.getProductsByColor(color, page, size, sortBy)
        );
    }

    @GetMapping("/material/{material}")
    public ResponseEntity<Page<ProductResponse>> getProductsByMaterial(
            @PathVariable String material,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {

        return ResponseEntity.ok(
                productService.getProductsByMaterial(material, page, size, sortBy)
        );
    }


    @GetMapping("/price")
    public ResponseEntity<Page<ProductResponse>> getProductsByPrice(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {

        return ResponseEntity.ok(
                productService.getProductsByPriceRange(min, max, page, size, sortBy)
        );
    }


    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponse>> filterProducts(
            ProductFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {

        return ResponseEntity.ok(
                productService.filterProducts(filter, page, size, sortBy)
        );
    }

    @GetMapping("/details/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(
            @PathVariable String slug
    ) {

        return ResponseEntity.ok(
                productService.getProductBySlug(slug)
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }
}
