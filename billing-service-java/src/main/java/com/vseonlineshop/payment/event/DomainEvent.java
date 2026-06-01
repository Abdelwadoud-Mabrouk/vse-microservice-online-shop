package com.vseonlineshop.payment.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;


public record DomainEvent<T>(
        UUID eventId,
        String eventType,
        String service,
        UUID correlationId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
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
                Instant.now(),
                payload
        );
    }
}