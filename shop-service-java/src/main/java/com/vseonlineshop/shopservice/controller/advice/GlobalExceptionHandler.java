package com.vseonlineshop.shopservice.controller.advice;

import com.vseonlineshop.shopservice.exception.BusinessException;
import com.vseonlineshop.shopservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {

        String detailedMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" | "));

        ErrorResponse errorBody = new ErrorResponse(
                "VALIDATION_FAILED",
                detailedMessage,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("[BUSINESS EXCEPTION] {}", ex.getMessage());

        ErrorResponse errorBody = new ErrorResponse(
                ex.getErrorCode() != null ? ex.getErrorCode() : "BUSINESS_ERROR",
                ex.getMessage(),
                LocalDateTime.now()
        );

        // ✅ ex.getHttpStatus() nutzen anstelle von ex.hashCode()!
        return ResponseEntity.status(ex.getHttpStatus()).body(errorBody);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("[SERVICE UNAVAILABLE] {}", ex.getMessage());

        ErrorResponse errorBody = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        // Logge den unvorhergesehenen Fehler für einfaches Debugging!
        log.error("[UNEXPECTED ERROR] Unerwarteter Systemfehler:", ex);

        ErrorResponse errorBody = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Ein unerwarteter Systemfehler ist aufgetreten.",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
    }
}