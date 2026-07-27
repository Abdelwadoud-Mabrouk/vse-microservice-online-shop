package com.vseonlineshop.shopservice.dto.order.response;



import com.vseonlineshop.shopservice.domain.OrderStatus;
import com.vseonlineshop.shopservice.dto.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailsResponse(
        UUID orderId,

        CustomerInformationResponse customer,

        ShippingAddressResponse shippingAddress,

        List<OrderItemResponse> items,

        BigDecimal totalAmount,

        PaymentMethod paymentMethod,

        OrderStatus status,

        LocalDateTime createdAt
) {}