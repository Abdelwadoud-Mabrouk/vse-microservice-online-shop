package com.vseonlineshop.productservicejava.controller;


import com.vseonlineshop.productservicejava.dto.ProductFilter;
import com.vseonlineshop.productservicejava.dto.ProductResponse;
import com.vseonlineshop.productservicejava.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    // toDo: Shop anpassen ==> Add Table of prodcuts + kafka
    //       Angular: Herro Page: All Prodcuten listen (Pagin), Filer mit Categorie, Header anapssen



    // ==========================================================
    // Landing Page
    // GET /api/products?page=0&size=12&sortBy=name
    // ==========================================================

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


    // ==========================================================
    // Kategorie
    // GET /api/products/category/sofas
    // ==========================================================

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


    // ==========================================================
    // Suche
    // GET /api/products/search?query=sofa
    // ==========================================================

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


    // ==========================================================
    // Farbe
    // GET /api/products/color/white
    // ==========================================================

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


    // ==========================================================
    // Material
    // GET /api/products/material/leather
    // ==========================================================

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


    // ==========================================================
    // Preis
    // GET /api/products/price?min=100&max=500
    // ==========================================================

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


    // ==========================================================
    // Kombinierte Filter
    // GET /api/products/filter?...alle Filter...
    // ==========================================================

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


    // ==========================================================
    // Produkt über Slug
    // GET /api/products/details/ecksofa-nuvola
    // ==========================================================

    @GetMapping("/details/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(
            @PathVariable String slug
    ) {

        return ResponseEntity.ok(
                productService.getProductBySlug(slug)
        );
    }


    // ==========================================================
    // Produkt über UUID
    // GET /api/products/{id}
    // ==========================================================

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        System.out.println("PRODUCT REQUEST: " + id);
        System.out.println("FROM IP: " + request.getRemoteAddr());
        System.out.println("USER AGENT: " + request.getHeader("User-Agent"));

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }
}
