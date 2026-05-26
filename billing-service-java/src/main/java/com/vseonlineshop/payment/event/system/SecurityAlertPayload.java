package com.vseonlineshop.payment.event.system;

public record SecurityAlertPayload(
        String alertId,
        String warningMessage,
        String invalidPaymentIdReceived
) {
}