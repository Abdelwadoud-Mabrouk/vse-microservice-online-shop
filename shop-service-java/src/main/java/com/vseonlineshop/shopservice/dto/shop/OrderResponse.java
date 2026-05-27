package com.vseonlineshop.shopservice.dto.shop;



import com.vseonlineshop.shopservice.domain.OrderStatus;
import com.vseonlineshop.shopservice.domain.PaymentProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID userId,
    String paymentId,
    OrderStatus status,
    BigDecimal totalAmount,
    String currency,
    PaymentProvider paymentProvider,
    List<OrderItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String approvalUrl
) {}