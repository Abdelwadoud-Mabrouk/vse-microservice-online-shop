package com.vseonlineshop.shopservice.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super("Order nicht gefunden: " + orderId);
    }
}