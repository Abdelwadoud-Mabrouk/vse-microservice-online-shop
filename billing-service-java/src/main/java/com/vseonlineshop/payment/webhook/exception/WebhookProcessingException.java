package com.vseonlineshop.payment.webhook.exception;

public class WebhookProcessingException extends WebhookException {
    public WebhookProcessingException(String message) {
        super(message, 500);
    }
}
