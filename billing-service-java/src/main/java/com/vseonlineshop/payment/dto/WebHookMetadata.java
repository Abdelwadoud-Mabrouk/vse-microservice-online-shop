package com.vseonlineshop.payment.dto;

public record WebHookMetadata(String paymentIdRaw, String orderId, String correlationId) {
}

