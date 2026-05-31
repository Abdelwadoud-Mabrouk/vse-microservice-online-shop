package com.vseonlineshop.payment.controller;


import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.vseonlineshop.payment.webhook.WebhookVerifier;
import com.vseonlineshop.payment.webhook.stripe.StripeWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeWebhookService webhookService;

    private final WebhookVerifier webhookVerifier;


    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;


        try {
            event = webhookVerifier.verifyStripe(payload, sigHeader);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        StripeObject stripeObject = event
                .getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (stripeObject == null) {
            return ResponseEntity.badRequest()
                    .body("Deserialization failed");
        }

        webhookService.handleStripeWebhook(event, stripeObject);

        return ResponseEntity.ok("Event erfolgreich verarbeitet");
    }
}