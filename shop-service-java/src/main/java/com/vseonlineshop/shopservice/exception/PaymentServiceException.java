package com.vseonlineshop.shopservice.exception;

public class PaymentServiceException extends RuntimeException {
    public PaymentServiceException(String message) {
        super(message);
    }
}