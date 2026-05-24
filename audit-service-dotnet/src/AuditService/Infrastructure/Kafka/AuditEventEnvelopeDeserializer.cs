using System.Text.Json;
using AuditService.Application.Events;

namespace AuditService.Infrastructure.Kafka;

public static class AuditEventEnvelopeDeserializer
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true
    };

    public static AuditEventEnvelope Deserialize(string message)
    {
        var envelope = JsonSerializer.Deserialize<AuditEventEnvelopeDto>(message, JsonOptions)
            ?? throw new JsonException("Audit event envelope is empty.");

        if (envelope.CorrelationId == Guid.Empty)
        {
            throw new JsonException("correlationId is required.");
        }

        if (string.IsNullOrWhiteSpace(envelope.EventType))
        {
            throw new JsonException("eventType is required.");
        }

        if (string.IsNullOrWhiteSpace(envelope.Service))
        {
            throw new JsonException("service is required.");
        }

        return new AuditEventEnvelope(
            envelope.EventId == Guid.Empty ? Guid.NewGuid() : envelope.EventId,
            envelope.CorrelationId,
            envelope.EventType.Trim(),
            envelope.Service.Trim(),
            envelope.Timestamp ?? DateTimeOffset.UtcNow,
            envelope.Payload ?? new Dictionary<string, object?>(),
            envelope.PreviousEventId,
            string.IsNullOrWhiteSpace(envelope.Actor) ? envelope.Service.Trim() : envelope.Actor.Trim(),
            string.IsNullOrWhiteSpace(envelope.StatusCode) ? "SUCCESS" : envelope.StatusCode.Trim());
    }

    private sealed record AuditEventEnvelopeDto(
        Guid EventId,
        Guid CorrelationId,
        string? EventType,
        string? Service,
        DateTimeOffset? Timestamp,
        Dictionary<string, object?>? Payload,
        Guid? PreviousEventId,
        string? Actor,
        string? StatusCode);
}
