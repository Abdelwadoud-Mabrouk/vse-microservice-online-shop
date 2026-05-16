namespace AuditService.Application;

public sealed record AuditSnapshotDto(
    Guid Id,
    Guid CorrelationId,
    string EventType,
    string Service,
    DateTimeOffset Timestamp,
    IReadOnlyDictionary<string, object?> Payload,
    Guid? PreviousEventId,
    string Actor,
    string StatusCode);
