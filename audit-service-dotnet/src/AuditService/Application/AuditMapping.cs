using AuditService.Domain;

namespace AuditService.Application;

internal static class AuditMapping
{
    public static AuditSnapshotDto ToDto(this AuditSnapshot snapshot)
    {
        return new AuditSnapshotDto(
            snapshot.Id,
            snapshot.CorrelationId,
            snapshot.EventType,
            snapshot.Service,
            snapshot.Timestamp,
            snapshot.Payload,
            snapshot.PreviousEventId,
            snapshot.Actor,
            snapshot.StatusCode);
    }
}
