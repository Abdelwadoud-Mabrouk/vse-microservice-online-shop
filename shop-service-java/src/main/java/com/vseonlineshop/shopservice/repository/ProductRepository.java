package com.vseonlineshop.shopservice.repository;

import com.vseonlineshop.shopservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
}