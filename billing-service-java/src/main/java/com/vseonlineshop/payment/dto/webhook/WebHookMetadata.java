package com.vseonlineshop.payment.dto.webhook;

public record WebHookMetadata(String paymentIdRaw, String orderId, String correlationId) {
}

