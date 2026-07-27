package com.vseonlineshop.payment.controller;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentResponse;
import com.vseonlineshop.payment.dto.refund.RefundRequestDto;
import com.vseonlineshop.payment.dto.refund.RefundResponseDto;
import com.vseonlineshop.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request, @RequestHeader("X-Correlation-Id") UUID correlationId) {


        PaymentResponse response = paymentService.initiatePayment(request, correlationId);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{correlationId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable java.util.UUID correlationId) {

        // TODO: PaymentService.getByCorrelationId() implementieren
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refund")
    public ResponseEntity<RefundResponseDto> processRefund(@RequestBody RefundRequestDto request, @RequestHeader("X-Correlation-Id") UUID correlationId) {
        String refundTransactionId = paymentService.processRefund(request, correlationId);

        return ResponseEntity.ok(new RefundResponseDto(
                correlationId,
                refundTransactionId,
                "REFUND_SUCCEEDED"
        ));
    }

    @GetMapping("/status/{transactionId}")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String transactionId, @RequestHeader("X-Correlation-Id") UUID correlationId) {


        return ResponseEntity.ok(Map.of(
                "transactionId", transactionId,
                "status", paymentService.getStatus(transactionId, correlationId)
        ));
    }
}
