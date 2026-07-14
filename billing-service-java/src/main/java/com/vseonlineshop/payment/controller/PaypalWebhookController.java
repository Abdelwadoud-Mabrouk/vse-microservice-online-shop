package com.vseonlineshop.payment.controller;


import com.vseonlineshop.payment.webhook.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaypalWebhookController {


    private final WebhookDispatcher webhookDispatcher;

    @PostMapping("/paypal")
    public ResponseEntity<String> paypalWebhook(
            @RequestHeader("PAYPAL-TRANSMISSION-ID") String transmissionId,
            @RequestHeader("PAYPAL-TRANSMISSION-TIME") String transmissionTime,
            @RequestHeader("PAYPAL-TRANSMISSION-SIG") String signature,
            @RequestHeader("PAYPAL-CERT-URL") String certUrl,
            @RequestHeader("PAYPAL-AUTH-ALGO") String authAlgo,
            @RequestBody String payload) {

        webhookDispatcher.dispatch("PAYPAL", payload, Map.of(
                "PAYPAL-TRANSMISSION-ID", transmissionId,
                "PAYPAL-TRANSMISSION-TIME", transmissionTime,
                "PAYPAL-TRANSMISSION-SIG", signature,
                "PAYPAL-CERT-URL", certUrl,
                "PAYPAL-AUTH-ALGO", authAlgo
        ));
        return ResponseEntity.ok("ok");
    }

}