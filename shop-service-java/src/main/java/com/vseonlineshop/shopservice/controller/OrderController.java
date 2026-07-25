package com.vseonlineshop.shopservice.controller;


import com.vseonlineshop.shopservice.dto.order.request.OrderRequest;
import com.vseonlineshop.shopservice.dto.order.response.CheckoutResponse;
import com.vseonlineshop.shopservice.dto.order.response.OrderDetailsResponse;
import com.vseonlineshop.shopservice.service.shop.OrderApplicationService;
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
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody OrderRequest request) {
        log.info("[OrderController] Neue Order-Anfrage, lastName={}, provider={}, items={}",
                request.customer().lastName(), request.paymentMethod(), request.items().size());

        CheckoutResponse response = orderApplicationService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Order per ID abrufen.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsResponse> getOrder(@PathVariable UUID id) {
        log.debug("[OrderController] Order {} abfragen", id);
        return ResponseEntity.ok(orderApplicationService.getOrder(id));
    }

    /**
     * Endpunkt für das Angular-Frontend.
     * POST /api/orders/capture/PAYPAL-TOKEN-HIER
     */
    @PostMapping("/capture/{token}")
    public ResponseEntity<Map<String, String>> capturePaypalOrder(@PathVariable String token) {

        Map<String, String> billingResponse = orderApplicationService.capturePaypal(token);

        return ResponseEntity.ok(billingResponse);
    }

}