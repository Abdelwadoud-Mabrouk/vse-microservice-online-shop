package com.vseonlineshop.payment.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final List<WebhookHandler> handlers;

    public void dispatch(String eventSource, String payload, Map<String, String> headers) {
        handlers.stream()
                .filter(h -> h.supports(eventSource))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kein Handler für: " + eventSource))
                .handle(payload, headers);
    }
}