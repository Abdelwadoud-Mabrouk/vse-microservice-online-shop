package com.vseonlineshop.payment.webhook.exception;

public class WebhookException extends RuntimeException {
    private final int httpStatus;

    public WebhookException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() { return httpStatus; }
}
