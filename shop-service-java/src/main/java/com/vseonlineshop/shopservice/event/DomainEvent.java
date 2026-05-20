package com.vseonlineshop.shopservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record DomainEvent<T>(
        UUID eventId,
        String eventType,
        String source,
        UUID correlationId,
        LocalDateTime occurredAt,
        T payload
) {
    public static <T> DomainEvent<T> of(
            String eventType,
            String source,
            UUID correlationId,
            T payload) {
        return new DomainEvent<>(
                UUID.randomUUID(),
                eventType,
                source,
                correlationId,
                LocalDateTime.now(),
                payload
        );
    }
}