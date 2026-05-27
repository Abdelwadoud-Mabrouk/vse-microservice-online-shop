package com.vseonlineshop.shopservice.event.dtos;

import java.time.Instant;
import java.util.Map;

public record SystemAlertPayload(
        String alertId,
        String serviceName,
        String contextKey,
        String errorMessage,
        Instant timestamp,
        Map<String, String> metadata
) {}