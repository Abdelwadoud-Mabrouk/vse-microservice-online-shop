package com.vseonlineshop.payment.webhook.papal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;


@JsonIgnoreProperties(ignoreUnknown = true)
public record PaypalWebhookPayload(

    @JsonProperty("id")
    String webhookEventId,

    @JsonProperty("event_type")
    String eventType,
    @JsonProperty("resource")
    PaypalResource resource,

    @JsonProperty("create_time")
    String createTime
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaypalResource(

        @JsonProperty("id")
        String providerTransactionId,

        @JsonProperty("status")
        String status,

        @JsonProperty("custom_id")
        String customId,

        @JsonProperty("amount")
        Map<String, String> amount,

        @JsonProperty("status_details")
        PaypalStatusDetails statusDetails

    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaypalStatusDetails(
        @JsonProperty("reason")
        String reason
    ) {}
}
