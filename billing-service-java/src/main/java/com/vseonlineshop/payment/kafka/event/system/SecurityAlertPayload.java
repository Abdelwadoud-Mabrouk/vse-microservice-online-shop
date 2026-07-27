package com.vseonlineshop.payment.kafka.event.system;

public record SecurityAlertPayload(
        String alertId,
        String warningMessage,
        String invalidPaymentIdReceived
) {
}