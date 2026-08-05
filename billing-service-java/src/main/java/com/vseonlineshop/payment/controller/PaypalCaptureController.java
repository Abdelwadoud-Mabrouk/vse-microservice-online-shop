package com.vseonlineshop.payment.controller;


import com.vseonlineshop.payment.service.PaypalCaptureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/paypal")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "PayPal Controller",
        description = "Provider-spezifische Endpunkte für PayPal"
)
public class PaypalCaptureController {

    private final PaypalCaptureService paypalCaptureService;

    @PostMapping("/capture/{token}")
    @Operation(
            summary = "PayPal-Zahlung abschließen",
            description = """
                    Führt den Capture einer zuvor vom Kunden bestätigten PayPal-Bestellung durch.
                    Dieser Endpunkt wird ausschließlich für den PayPal-Checkout verwendet.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "PayPal-Zahlung erfolgreich abgeschlossen"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ungültiger oder abgelaufener PayPal-Token"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "PayPal-Bestellung nicht gefunden"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Fehler bei der Kommunikation mit der PayPal API"
            )
    })
    public ResponseEntity<Map<String,String>> captureOrder(
            @Parameter(
                    description = "PayPal-Order-Token, der nach der Kundenbestätigung zurückgegeben wird",
                    required = true,
                    example = "5O190127TN364715T"
            )
            @PathVariable String token
    ) {

        String result = paypalCaptureService.captureOrder(token);

        return ResponseEntity.ok(
                Map.of(
                        "message", result
                )
        );
    }
}
