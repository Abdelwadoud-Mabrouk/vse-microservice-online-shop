package com.vseonlineshop.payment.strategy;

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.ProviderResponse;
import com.vseonlineshop.payment.enums.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class StripePaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentProvider provider) {
        return provider == PaymentProvider.STRIPE;
    }

    @Override
    public ProviderResponse processPayment(PaymentRequest request) {

        log.info("[STRIPE] Initializing payment for order={}",
                request.orderId());

        // =========================================================
        // MUST-DO 1: Minor Units (EUR -> Cent)
        // Stripe erwartet Beträge in der kleinsten Währungseinheit
        // =========================================================

        long amountInCents = request.totalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        // =========================================================
        // MUST-DO 2: Idempotency Key
        // Verhindert doppelte Abbuchungen bei Retries
        // =========================================================

        String idempotencyKey =
                request.correlationId().toString();

        log.info("""
                        [STRIPE]
                        amountInCents={}
                        currency={}
                        idempotencyKey={}
                        """,
                amountInCents,
                request.currency(),
                idempotencyKey
        );

        // =========================================================
        // TODO: Real Stripe SDK Integration
        // =========================================================

        /*
        Stripe.apiKey = stripeApiKey;

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(amountInCents)
                        .setCurrency(request.currency().toLowerCase())
                        .build();

        RequestOptions options =
                RequestOptions.builder()
                        .setIdempotencyKey(idempotencyKey)
                        .build();

        PaymentIntent paymentIntent =
                PaymentIntent.create(params, options);
        */

        // =========================================================
        // MOCK Stripe Response
        // =========================================================

        String providerTransactionId =
                "pi_" + UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        String approvalUrl =
                "https://checkout.stripe.com/pay/"
                        + providerTransactionId;

        String rawResponse = """
                {
                  "id": "%s",
                  "object": "payment_intent",
                  "status": "requires_payment_method",
                  "checkout_url": "%s"
                }
                """.formatted(
                providerTransactionId,
                approvalUrl
        );

        log.info("""
                        [STRIPE]
                        payment session created
                        providerTransactionId={}
                        """,
                providerTransactionId
        );

        return new ProviderResponse(
                false,
                providerTransactionId,
                approvalUrl,
                rawResponse,
                null
        );
    }

}


