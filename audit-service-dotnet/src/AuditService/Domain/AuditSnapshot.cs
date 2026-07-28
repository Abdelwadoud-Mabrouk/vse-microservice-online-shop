namespace AuditService.Domain;

public sealed record AuditSnapshot(
    Guid Id,
    Guid CorrelationId,
    string EventType,
    string Service,
    DateTimeOffset Timestamp,
    IReadOnlyDictionary<string, object?> Payload,
    Guid? PreviousEventId,
    string Actor,
    string StatusCode)
{
    public static AuditSnapshot Create(
        Guid correlationId,
        string eventType,
        string service,
        IReadOnlyDictionary<string, object?> payload,
        Guid? previousEventId,
        string actor,
        string statusCode,
        Guid? eventId = null)
    {
        if (correlationId == Guid.Empty)
        {
            throw new ArgumentException("Correlation id must not be empty.", nameof(correlationId));
        }

        return new AuditSnapshot(
            eventId is null || eventId.Value == Guid.Empty ? Guid.NewGuid() : eventId.Value,
            correlationId,
            Required(eventType, nameof(eventType)),
            Required(service, nameof(service)),
            DateTimeOffset.UtcNow,
            payload,
            previousEventId,
            string.IsNullOrWhiteSpace(actor) ? "system" : actor.Trim(),
            Required(statusCode, nameof(statusCode)));
    }

    private static string Required(string value, string name)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new ArgumentException($"{name} is required.", name);
        }

        return value.Trim();
    }
}
