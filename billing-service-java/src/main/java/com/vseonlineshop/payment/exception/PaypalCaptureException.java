package com.vseonlineshop.payment.exception;

public class PaypalCaptureException extends RuntimeException {
    public PaypalCaptureException(String message) {
        super(message);
    }
}
