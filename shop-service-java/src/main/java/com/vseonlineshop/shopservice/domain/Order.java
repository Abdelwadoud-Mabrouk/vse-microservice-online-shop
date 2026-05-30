package com.vseonlineshop.shopservice.domain;


import com.vseonlineshop.shopservice.dto.payment.PaymentMethod;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, unique = true)
    private UUID correlationId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "payment_id", unique = true)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_provider", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private String currency = "EUR";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Helper method for Bi-directional mapping
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}