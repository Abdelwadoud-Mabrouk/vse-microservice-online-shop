package com.vseonlineshop.payment.controller;


import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.vseonlineshop.payment.webhook.stripe.StripeWebhookService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {

    @Autowired
    StripeWebhookService webhookService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    private StripeClient stripeClient;

    @PostConstruct
    public void init() {
        this.stripeClient = new StripeClient(stripeSecretKey);
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        // 1. Sicherheit: Verifizieren, dass das Event WIRKLICH von Stripe kommt
        try {
            if (endpointSecret != null && sigHeader != null) {
                event = stripeClient.constructEvent(payload, sigHeader, endpointSecret);
            } else {
                System.err.println(" Webhook-Secret oder Signatur-Header fehlt!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing secret or signature");
            }
        } catch (IllegalArgumentException e) {
            System.err.println(" Ungültiges Payload-Format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        } catch (SignatureVerificationException e) {
            System.err.println(" Signatur-Prüfung fehlgeschlagen! Event ist manipuliert oder Secret falsch.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // 2. Das Event-Objekt auspacken
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);

        if (stripeObject == null) {
            System.err.println(" Deserialisierung fehlgeschlagen für Event: " + event.getType());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Deserialization failed");
        }

        webhookService.handleStripeWebhook(event, stripeObject);


        // Ganz wichtig: Stripe MUSS immer ein HTTP 200 OK bekommen,
        // sonst denkt Stripe, dein Server ist offline und schickt das Event tagelang immer wieder!
        return ResponseEntity.ok("Event erfolgreich verarbeitet");
    }
}