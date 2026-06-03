package com.vseonlineshop.shopservice.config;


import com.vseonlineshop.shopservice.domain.Product;
import com.vseonlineshop.shopservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final ProductRepository productRepository;

    public DatabaseSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        long productCount = productRepository.count();

        if (productCount == 0) {
            log.info("PostgreSQL 'products' table is empty. Starting database seeding...");

            Product bed = new Product();
            bed.setId(UUID.fromString("55555555-5555-4555-8555-555555555555"));
            bed.setSku("SKU-BED-LINA");
            bed.setName("Doppelbett Lina 180x200");
            bed.setCategory("Betten");
            bed.setPrice(new BigDecimal("599.99"));

            Product desk = new Product();
            desk.setId(UUID.fromString("22222222-2222-4222-8222-222222222222"));
            desk.setSku("SKU-DESK-OSLO");
            desk.setName("Schreibtisch Oslo");
            desk.setCategory("Tische");
            desk.setPrice(new BigDecimal("149.50"));

            Product lamp = new Product();
            lamp.setId(UUID.fromString("33333333-3333-4333-8333-333333333333"));
            lamp.setSku("SKU-LAMP-LUNA");
            lamp.setName("Stehlampe Luna");
            lamp.setCategory("Lampen");
            lamp.setPrice(new BigDecimal("79.95"));

            Product product = new Product();
            product.setId(UUID.fromString("11111111-1111-4111-8111-111111111111"));
            product.setSku("SKU-CHAIR-OAK");
            product.setName("test");
            product.setCategory("test");
            product.setPrice(new BigDecimal("79.95"));

            productRepository.saveAll(List.of(bed, desk, lamp, product));

            log.info("[SUCCESS] Database successfully seeded with 3 test products.");
        } else {
            log.info("Database seeding skipped. 'products' table already contains {} items.", productCount);
        }
    }
}
