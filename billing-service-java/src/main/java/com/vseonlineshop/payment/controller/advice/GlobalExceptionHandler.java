package com.vseonlineshop.payment.controller.advice;


import com.vseonlineshop.payment.exception.PaymentSessionException;
import com.vseonlineshop.payment.exception.PaypalCaptureException;
import com.vseonlineshop.payment.webhook.exception.WebhookProcessingException;
import com.vseonlineshop.payment.webhook.exception.WebhookSignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;


@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(PaymentSessionException.class)
    public ResponseEntity<ErrorResponse> handlePaymentSessionException(PaymentSessionException ex) {
        ErrorResponse errorBody = new ErrorResponse(
                "PAYMENT_FAILED",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorBody);
    }

    @ExceptionHandler(WebhookSignatureException.class)
    public ResponseEntity<ErrorResponse> handleWebhookSignatureException(WebhookSignatureException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse("WEBHOOK_SIGNATURE_INVALID", ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(WebhookProcessingException.class)
    public ResponseEntity<ErrorResponse> handleWebhookProcessingException(WebhookProcessingException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse("WEBHOOK_PROCESSING_ERROR", ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(PaypalCaptureException.class)
    public ResponseEntity<ErrorResponse> handlePaypalCaptureException(PaypalCaptureException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse("CAPTURE_PROCESSING_ERROR", ex.getMessage(), LocalDateTime.now())
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse errorBody = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Ein unerwarteter Systemfehler ist aufgetreten.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
    }
}