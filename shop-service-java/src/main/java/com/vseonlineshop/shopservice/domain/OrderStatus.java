package com.vseonlineshop.shopservice.domain;

public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    PROCESSING,
    COMPLETED,
    PAYMENT_FAILED,
    OUT_OF_STOCK,
    ROLLBACK_COMPLETED,
    REFUND_PENDING,
    CANCELLED
}