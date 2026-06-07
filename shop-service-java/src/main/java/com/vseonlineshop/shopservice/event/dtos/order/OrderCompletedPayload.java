package com.vseonlineshop.shopservice.event.dtos.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCompletedPayload(
        UUID orderId,
        UUID userId,
        String email,
        List<InvoiceItem> items,
        BigDecimal amount,
        String currency,
        UUID correlationId,
        String transactionId,
        CustomerPayload customer,
        String paymentProvider
) {}
