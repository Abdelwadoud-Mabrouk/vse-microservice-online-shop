package com.vseonlineshop.payment.event.system;

public record SystemAlertPayload(
        String alertId,
        String errorMessage,
        String providerTransactionId
) {}