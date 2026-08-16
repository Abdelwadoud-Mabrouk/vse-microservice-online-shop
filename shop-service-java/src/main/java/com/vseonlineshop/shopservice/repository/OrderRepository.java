package com.vseonlineshop.shopservice.repository;



import com.vseonlineshop.shopservice.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface  OrderRepository extends JpaRepository<Order, UUID> {
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    Optional<Order> findByIdempotencyKey(UUID idempotencyKey);
}