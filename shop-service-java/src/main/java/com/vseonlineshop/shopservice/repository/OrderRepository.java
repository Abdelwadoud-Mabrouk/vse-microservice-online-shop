package com.vseonlineshop.shopservice.repository;



import com.vseonlineshop.shopservice.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByCorrelationId(UUID correlationId);
}