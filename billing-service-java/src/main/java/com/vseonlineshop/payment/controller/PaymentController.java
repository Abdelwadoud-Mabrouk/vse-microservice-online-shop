package com.vseonlineshop.payment.controller;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentResponse;
import com.vseonlineshop.payment.dto.refund.RefundRequestDto;
import com.vseonlineshop.payment.dto.refund.RefundResponseDto;
import com.vseonlineshop.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Payment Controller", description = "Endpunkte für Zahlungsabwicklung, Statusabfragen und Rückerstattungen")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    @Operation(
            summary = "Zahlung aufgeben",
            description = "Initiiert einen neuen Zahlungsvorgang über den gewählten Payment-Provider."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Zahlung akzeptiert und in Bearbeitung",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ungültige Request-Daten oder Validierungsfehler"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity – Transaktion konnte nicht verarbeitet werden"),
            @ApiResponse(responseCode = "503", description = "Zahlungsanbieter nicht erreichbar")
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @Parameter(description = "Eindeutige Trace-ID für das verteilte Tracing", required = true)
            @RequestHeader("X-Correlation-Id") UUID correlationId) {

        PaymentResponse response = paymentService.initiatePayment(request, correlationId);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{correlationId}")
    @Operation(
            summary = "Zahlungsstatus per Correlation-ID abrufen",
            description = "Sucht eine bestehende Zahlung anhand der Correlation-ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Zahlungsstatus erfolgreich abgerufen",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Keine Zahlung zu dieser Correlation-ID gefunden")
    })
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @Parameter(description = "UUID der Correlation-ID", required = true)
            @PathVariable UUID correlationId) {
        return ResponseEntity.ok(paymentService.getByCorrelationId(correlationId));
    }

    @PostMapping("/refund")
    @Operation(
            summary = "Rückerstattung verarbeiten",
            description = "Führt eine Rückerstattung (Refund) für eine Transaktion durch."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rückerstattung erfolgreich verarbeitet",
                    content = @Content(schema = @Schema(implementation = RefundResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Ungültige Refund-Daten"),
            @ApiResponse(responseCode = "404", description = "Ursprüngliche Transaktion nicht gefunden"),
            @ApiResponse(responseCode = "502", description = "Fehler bei der Kommunikation mit dem Zahlungsanbieter")
    })
    public ResponseEntity<RefundResponseDto> processRefund(
            @Valid @RequestBody RefundRequestDto request,
            @Parameter(description = "Eindeutige Trace-ID für das verteilte Tracing", required = true)
            @RequestHeader("X-Correlation-Id") UUID correlationId) {

        String refundTransactionId = paymentService.processRefund(request, correlationId);

        return ResponseEntity.ok(new RefundResponseDto(
                correlationId,
                refundTransactionId,
                "REFUND_SUCCEEDED"
        ));
    }

    @GetMapping("/status/{transactionId}")
    @Operation(
            summary = "Status einer Transaktion abrufen",
            description = "Liefert den aktuellen Status einer spezifischen Transaktions-ID zurück."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status erfolgreich ermittelt"),
            @ApiResponse(responseCode = "404", description = "Transaktions-ID nicht gefunden")
    })
    public ResponseEntity<Map<String, String>> getStatus(
            @Parameter(description = "Die ID der Transaktion", required = true)
            @PathVariable String transactionId,
            @Parameter(description = "Eindeutige Trace-ID für das verteilte Tracing", required = true)
            @RequestHeader("X-Correlation-Id") UUID correlationId) {

        return ResponseEntity.ok(Map.of(
                "transactionId", transactionId,
                "status", paymentService.getStatus(transactionId, correlationId)
        ));
    }
}