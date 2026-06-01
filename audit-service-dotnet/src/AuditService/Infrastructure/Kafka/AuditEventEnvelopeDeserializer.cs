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

        var service = string.IsNullOrWhiteSpace(envelope.Service)
            ? envelope.Source
            : envelope.Service;

        if (string.IsNullOrWhiteSpace(service))
        {
            throw new JsonException("service or source is required.");
        }

        var timestamp = ParseTimestamp(envelope.Timestamp)
            ?? ParseTimestamp(envelope.OccurredAt)
            ?? DateTimeOffset.UtcNow;

        return new AuditEventEnvelope(
            envelope.EventId == Guid.Empty ? Guid.NewGuid() : envelope.EventId,
            envelope.CorrelationId,
            envelope.EventType.Trim(),
            service.Trim(),
            timestamp,
            envelope.Payload ?? new Dictionary<string, object?>(),
            envelope.PreviousEventId,
            string.IsNullOrWhiteSpace(envelope.Actor) ? service.Trim() : envelope.Actor.Trim(),
            string.IsNullOrWhiteSpace(envelope.StatusCode) ? "SUCCESS" : envelope.StatusCode.Trim());
    }

    private static DateTimeOffset? ParseTimestamp(string? timestamp)
    {
        if (string.IsNullOrWhiteSpace(timestamp))
        {
            return null;
        }

        return DateTimeOffset.TryParse(timestamp, out var parsed)
            ? parsed
            : null;
    }

    private sealed record AuditEventEnvelopeDto(
        Guid EventId,
        Guid CorrelationId,
        string? EventType,
        string? Service,
        string? Source,
        string? Timestamp,
        string? OccurredAt,
        Dictionary<string, object?>? Payload,
        Guid? PreviousEventId,
        string? Actor,
        string? StatusCode);
}
