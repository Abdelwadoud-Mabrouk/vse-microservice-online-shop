package com.vseonlineshop.payment.webhook.stripe;

import java.util.Map;

public record StripeWebhookPayload(
        String type,
        StripeData data
) {
    public record StripeData(
            StripeObject object
    ) {
    }

    public record StripeObject(
            String id,
            Long amount,
            String currency,
            Map<String, String> metadata
    ) {
    }
}