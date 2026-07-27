package com.vseonlineshop.payment.kafka.event.system;

public record SystemAlertPayload(
        String alertId,
        String errorMessage,
        String providerTransactionId
) {}