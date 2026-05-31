package com.vseonlineshop.payment.webhook;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prüft ob Webhook wirklich vom Provider kommt (nicht gefälscht).
 * Jeder Provider hat eigenes Signatur-Verfahren.
 */
@Slf4j
@Component
public class WebhookVerifier {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    private StripeClient stripeClient;

    @PostConstruct
    public void init() {
        this.stripeClient = new StripeClient(stripeSecretKey);
    }


    public Event verifyStripe(String payload, String sigHeader)
            throws SignatureVerificationException {

        if (sigHeader == null) {
            throw new IllegalArgumentException("Missing Stripe-Signature header");
        }

        return stripeClient.constructEvent(
                payload,
                sigHeader,
                endpointSecret
        );
    }
}
