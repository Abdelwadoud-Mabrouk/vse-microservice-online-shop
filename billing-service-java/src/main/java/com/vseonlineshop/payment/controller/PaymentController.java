package com.vseonlineshop.payment.controller;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentResponse;
import com.vseonlineshop.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /payments
     * Nimmt Payment-Request entgegen, legt Payment als PENDING an,
     * startet Async-Verarbeitung → gibt sofort 202 zurück.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * GET /payments/{correlationId}
     * Polling-Endpoint: Shop fragt Status ab (PENDING → CONFIRMED/FAILED)
     */
    @GetMapping("/{correlationId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable java.util.UUID correlationId) {

        // TODO: PaymentService.getByCorrelationId() implementieren
        return ResponseEntity.ok().build();
    }
}
