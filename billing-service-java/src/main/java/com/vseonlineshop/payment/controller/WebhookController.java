package com.vseonlineshop.payment.controller;

import com.vseonlineshop.payment.exception.PaymentServiceException;
import com.vseonlineshop.payment.webhook.WebhookService;
import com.vseonlineshop.payment.webhook.WebhookVerifier;
import com.vseonlineshop.payment.webhook.papal.PaypalWebhookPayload;
import com.vseonlineshop.payment.webhook.stripe.StripeWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook-Endpoints für externe Provider.
 * PayPal/Klarna/Stripe rufen diese URLs auf — konfiguriert im Provider-Dashboard.
 * <p>
 * Wichtig: IMMER 200 zurückgeben (auch bei Fehler) → sonst retried Provider endlos!
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final WebhookVerifier webhookVerifier;

    // ──────────────────────────────────────────────────────
    //  POST /webhook/paypal
    //  Konfiguriert in PayPal Developer Dashboard:
    //  https://developer.paypal.com/dashboard/webhooks
    //
    //  PayPal schickt:
    //  Header: PAYPAL-TRANSMISSION-ID, PAYPAL-TRANSMISSION-SIG, ...
    //  Body:   PaypalWebhookPayload (event_type + resource)
    // ──────────────────────────────────────────────────────
    @PostMapping("/paypal")
    public ResponseEntity<Void> handlePaypal(
            @RequestBody PaypalWebhookPayload payload,

            // PayPal Signatur-Header (Webhook-Verifizierung)
            @RequestHeader("PAYPAL-TRANSMISSION-ID") String transmissionId,
            @RequestHeader("PAYPAL-TRANSMISSION-SIG") String transmissionSig,
            @RequestHeader("PAYPAL-CERT-URL") String certUrl,
            @RequestHeader("PAYPAL-AUTH-ALGO") String authAlgo) {

        log.info("[WEBHOOK/PAYPAL] Eingehend event={} webhookEventId={}",
                payload.eventType(), payload.webhookEventId());

        // ── Schritt 1: Signatur prüfen (ist das wirklich PayPal?) ──
        boolean isValid = webhookVerifier.verifyPaypal(
                transmissionId, transmissionSig, certUrl, authAlgo, payload);

        if (!isValid) {
            log.warn("[WEBHOOK/PAYPAL] Ungültige Signatur! transmissionId={}", transmissionId);
            // 200 zurück — nicht 401! PayPal würde sonst retrien
            return ResponseEntity.ok().build();
        }

        // ── Schritt 2: Verarbeitung (DB + Kafka) ───────────────────
        try {
            webhookService.handlePaypalWebhook(payload);
        } catch (Exception ex) {
            // Fehler loggen aber 200 zurück → kein endloser Retry von PayPal
            log.error("[WEBHOOK/PAYPAL] Verarbeitung fehlgeschlagen: {}", ex.getMessage(), ex);
        }

        // ── Schritt 3: IMMER 200 an PayPal ────────────────────────
        // PayPal erwartet 200 innerhalb 30s, sonst Retry (bis zu 8x!)
        return ResponseEntity.ok().build();
    }

    // ──────────────────────────────────────────────────────
    //  POST /webhook/klarna  (Erweiterbar)
    // ──────────────────────────────────────────────────────
    @PostMapping("/klarna")
    public ResponseEntity<Void> handleKlarna(
            @RequestBody StripeWebhookPayload payload,
            @RequestBody String rawBody,
            @RequestHeader("Klarna-Signature") String signature) {

        log.info("[WEBHOOK/KLARNA] Eingehend");
        // TODO: KlarnaWebhookPayload + WebhookService.handleKlarnaWebhook()
        return ResponseEntity.ok().build();
    }

    // ──────────────────────────────────────────────────────
    //  POST /webhook/stripe
    // ──────────────────────────────────────────────────────
    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripe(
            @RequestBody StripeWebhookPayload payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {

        log.info("[WEBHOOK/STRIPE] Webhook-Notification empfangen. Event-Typ: {}", payload.type());

        try {
            webhookService.handleStripeWebhook(payload);

        } catch (PaymentServiceException ex) {
            // Fall B: FACHLICHER FEHLER (Business Exception, z.B. ID unbekannt)
            // Das wird sich durch einen Retry nicht ändern.
            log.error("[WEBHOOK/STRIPE] Fachlicher Fehler (Kein Retry nötig): {}", ex.getMessage());

            // Wir antworten TROTZDEM mit 200 OK, damit Stripe den Vorgang für immer abhakt!
            return ResponseEntity.ok().build();

        } catch (Exception ex) {
            // WICHTIG: Wir fangen alle Exceptions ab und loggen sie.
            // Wir antworten Stripe TROTZDEM mit 200 OK. Warum?
            // Wenn wir einen Fehler (500) senden, versucht Stripe es tagelang im Minutentakt erneut,
            // obwohl der Fehler (z.B. eine unbekannte ID) sich nicht von alleine lösen wird.
            log.error("[WEBHOOK/STRIPE] Kritischer Fehler bei der Verarbeitung: {}", ex.getMessage(), ex);
        }

        return ResponseEntity.ok().build();
    }
}
