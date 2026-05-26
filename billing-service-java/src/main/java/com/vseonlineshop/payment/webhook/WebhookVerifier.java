package com.vseonlineshop.payment.webhook;

import com.vseonlineshop.payment.webhook.papal.PaypalWebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Prüft ob Webhook wirklich vom Provider kommt (nicht gefälscht).
 * Jeder Provider hat eigenes Signatur-Verfahren.
 */
@Slf4j
@Component
public class WebhookVerifier {

    /**
     * PayPal Signatur-Verifizierung.
     * In Production: PayPal SDK WebhookEvent.validateReceivedEvent() verwenden.
     * Doku: https://developer.paypal.com/docs/api-basics/notifications/webhooks/notification-messages/
     */
    public boolean verifyPaypal(String transmissionId,
                                 String transmissionSig,
                                 String certUrl,
                                 String authAlgo,
                                 PaypalWebhookPayload payload) {
        // TODO Production:
        // APIContext apiContext = new APIContext(clientId, clientSecret, "live");
        // WebhookEvent.validateReceivedEvent(apiContext, headers, body, webhookId);

        // Sandbox/Test: immer true
        log.debug("[VERIFIER] PayPal Signatur-Check transmissionId={}", transmissionId);
        return true;
    }

    /**
     * Stripe Signatur-Verifizierung.
     * In Production: Webhook.constructEvent(payload, sigHeader, endpointSecret)
     */
    public boolean verifyStripe(String rawBody, String sigHeader) {
        // TODO Production:
        // Event event = Webhook.constructEvent(rawBody, sigHeader, endpointSecret);
        return true;
    }
}
