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
public class PaypalPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentProvider provider) {
        return provider == PaymentProvider.PAYPAL;
    }

    @Override
    public ProviderResponse processPayment(PaymentRequest request) {
        log.info("[PAYPAL] Processing payment for order={} amount={}{}",
            request.orderId(), request.totalAmount(), request.currency());

        // Must-DO: Minor Units → EUR Cent
        long amountInCents = request.totalAmount()
            .multiply(BigDecimal.valueOf(100))
            .longValueExact();

        // Must-DO: Idempotency-Key = correlationId
        String idempotencyKey = request.correlationId().toString();

        log.info("[PAYPAL] Sending amount={} cents, idempotencyKey={}", amountInCents, idempotencyKey);

        // TODO: echter PayPal SDK-Call
        // PayPalHttpClient client = new PayPalHttpClient(environment);
        // OrdersCreateRequest req = new OrdersCreateRequest();
        // req.header("PayPal-Request-Id", idempotencyKey);

        // Simulierte erfolgreiche Antwort:
        log.info("[STRIPE] Creating payment session for order={}",
                request.orderId());

        // Fake Provider Transaction ID
        String providerTransactionId =
                "pi_" + UUID.randomUUID().toString().replace("-", "");

        // Fake Redirect URL (Stripe Checkout)
        String approvalUrl =
                "https://checkout.stripe.com/pay/" + providerTransactionId;

        // Simulierte rohe Provider-Antwort
        String rawResponse = """
        {
          "id": "%s",
          "object": "payment_intent",
          "status": "requires_payment_method",
          "checkout_url": "%s"
        }
        """.formatted(providerTransactionId, approvalUrl);

        return new ProviderResponse(
                false,
                providerTransactionId,
                approvalUrl,
                rawResponse,
                null
        );
    }
}
