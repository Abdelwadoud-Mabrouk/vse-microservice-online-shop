package com.vseonlineshop.shopservice.controller;

import com.vseonlineshop.shopservice.dto.order.request.OrderRequest;
import com.vseonlineshop.shopservice.dto.order.response.CheckoutResponse;
import com.vseonlineshop.shopservice.dto.order.response.OrderDetailsResponse;
import com.vseonlineshop.shopservice.service.shop.OrderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Slf4j
@CrossOrigin("*")
@Tag(name = "Order Controller", description = "Endpunkte für den Bestellprozess und Zahlungsabwicklung")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping
    @Operation(summary = "Bestellung erstellen", description = "Erstellt eine neue Bestellung, reserviert Bestand und initiiert die Zahlung.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order erfolgreich angelegt"),
            @ApiResponse(responseCode = "400", description = "Ungültige Request-Daten"),
            @ApiResponse(responseCode = "409", description = "Kein Bestand im Warehouse verfügbar"),
            @ApiResponse(responseCode = "503", description = "Zahlungs- oder Lager-Service nicht erreichbar")
    })
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody OrderRequest request) {
        log.info("[OrderController] Neue Order-Anfrage, lastName={}, provider={}, items={}",
                request.customer().lastName(), request.paymentMethod(), request.items().size());

        CheckoutResponse response = orderApplicationService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Order per ID abrufen")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order gefunden"),
            @ApiResponse(responseCode = "404", description = "Order nicht gefunden")
    })
    public ResponseEntity<OrderDetailsResponse> getOrder(
            @Parameter(description = "UUID der Bestellung") @PathVariable UUID id) {
        log.debug("[OrderController] Order {} abfragen", id);
        return ResponseEntity.ok(orderApplicationService.getOrder(id));
    }

    @PostMapping("/capture/{token}")
    @Operation(summary = "PayPal-Zahlung abschließen", description = "Schließt eine PayPal-Transaktion über das Token ab.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Zahlung erfolgreich erfasst"),
            @ApiResponse(responseCode = "400", description = "Ungültiger PayPal Token")
    })
    public ResponseEntity<Map<String, String>> capturePaypalOrder(
            @Parameter(description = "PayPal Token") @PathVariable String token) {

        Map<String, String> billingResponse = orderApplicationService.capturePaypal(token);
        return ResponseEntity.ok(billingResponse);
    }
}