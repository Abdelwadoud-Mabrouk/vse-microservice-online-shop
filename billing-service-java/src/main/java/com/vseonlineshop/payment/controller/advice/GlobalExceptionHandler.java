package com.vseonlineshop.payment.controller.advice;


import com.vseonlineshop.payment.exception.PaymentSessionException;
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