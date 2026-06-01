package com.vseonlineshop.shopservice.controller;


import com.vseonlineshop.shopservice.dto.shop.OrderRequest;
import com.vseonlineshop.shopservice.dto.shop.OrderResponse;
import com.vseonlineshop.shopservice.service.shop.OrderApplicationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody OrderRequest request) {
        log.info("[OrderController] Neue Order-Anfrage, userId={}, provider={}, items={}",
                request.userId(), request.paymentMethod(), request.items().size());

        OrderResponse response = orderApplicationService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Order per ID abrufen.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        log.debug("[OrderController] Order {} abfragen", id);
        return ResponseEntity.ok(orderApplicationService.getOrder(id));
    }

}