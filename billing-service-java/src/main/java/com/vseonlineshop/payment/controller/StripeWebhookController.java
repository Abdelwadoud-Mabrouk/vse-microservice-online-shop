package com.vseonlineshop.payment.controller;


import com.vseonlineshop.payment.webhook.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {


    private final WebhookDispatcher webhookDispatcher;

    @PostMapping("/stripe")
    public ResponseEntity<String> stripeWebhook(
            @RequestHeader("Stripe-Signature") String sig,
            @RequestBody String payload) {
        webhookDispatcher.dispatch("STRIPE", payload, Map.of("Stripe-Signature", sig));
        return ResponseEntity.ok("ok");
    }
}