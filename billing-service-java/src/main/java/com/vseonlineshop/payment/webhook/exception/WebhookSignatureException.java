package com.vseonlineshop.payment.webhook.exception;

public class WebhookSignatureException extends WebhookException {

    public WebhookSignatureException(String message) {
        super(message, 400);
    }

}
