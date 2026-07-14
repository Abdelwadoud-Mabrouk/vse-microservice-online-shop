package com.vseonlineshop.payment.webhook;

import java.util.Map;

public interface WebhookHandler {
    boolean supports(String eventSource);

    void handle(String payload, Map<String, String> headers);
}