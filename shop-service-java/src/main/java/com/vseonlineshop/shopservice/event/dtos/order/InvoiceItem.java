package com.vseonlineshop.shopservice.event.dtos.order;

import java.math.BigDecimal;

public record InvoiceItem(String sku, String name, Integer quantity,  BigDecimal unitPrice) {
}
