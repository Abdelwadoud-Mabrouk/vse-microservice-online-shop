package com.vseonlineshop.payment.controller;


import com.vseonlineshop.payment.service.PaypalCaptureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/paypal")
@RequiredArgsConstructor
@Slf4j
public class PaypalCaptureController {

    private final PaypalCaptureService paypalCaptureService;

    @PostMapping("/capture/{orderId}")
    public ResponseEntity<String> captureOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(paypalCaptureService.captureOrder(orderId));

    }
}
