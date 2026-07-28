namespace AuditService.Application;

public sealed record CreateAuditSnapshotCommand(
    Guid CorrelationId,
    string EventType,
    string Service,
    IReadOnlyDictionary<string, object?> Payload,
    Guid? PreviousEventId,
    string Actor,
    string StatusCode,
    Guid? EventId = null);
