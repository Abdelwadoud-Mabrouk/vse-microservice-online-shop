namespace AuditService.Infrastructure.Persistence.Entities;

public sealed class AuditSnapshotEntity
{
    public Guid Id { get; init; }

    public Guid CorrelationId { get; init; }

    public string EventType { get; init; } = string.Empty;

    public string Service { get; init; } = string.Empty;

    public DateTimeOffset TimestampUtc { get; init; }

    public string PayloadJson { get; init; } = "{}";

    public Guid? PreviousEventId { get; init; }

    public string Actor { get; init; } = string.Empty;

    public string StatusCode { get; init; } = string.Empty;
}
