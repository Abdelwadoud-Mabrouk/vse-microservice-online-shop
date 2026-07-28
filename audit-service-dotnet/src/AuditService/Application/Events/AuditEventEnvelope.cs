namespace AuditService.Application.Events;

public sealed record AuditEventEnvelope(
    Guid EventId,
    Guid CorrelationId,
    string EventType,
    string Service,
    DateTimeOffset Timestamp,
    IReadOnlyDictionary<string, object?> Payload,
    Guid? PreviousEventId,
    string Actor,
    string StatusCode)
{
    public CreateAuditSnapshotCommand ToCommand()
    {
        return new CreateAuditSnapshotCommand(
            CorrelationId,
            EventType,
            Service,
            Payload,
            PreviousEventId,
            Actor,
            StatusCode,
            EventId);
    }
}
