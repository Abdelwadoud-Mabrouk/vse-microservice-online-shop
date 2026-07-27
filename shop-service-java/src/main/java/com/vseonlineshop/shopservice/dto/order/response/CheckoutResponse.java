package com.vseonlineshop.shopservice.dto.order.response;

import com.vseonlineshop.shopservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutResponse(

        UUID orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        String approvalUrl

) {
}