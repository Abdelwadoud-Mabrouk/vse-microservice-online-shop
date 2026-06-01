package com.vseonlineshop.shopservice.event.dtos.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InvoiceRequestedPayload(
        UUID orderId,
        UUID userId,
        String email,
        List<InvoiceItem> items,
        BigDecimal amount,
        String currency,
        UUID correlationId,
        CustomerPayload customer
) {}