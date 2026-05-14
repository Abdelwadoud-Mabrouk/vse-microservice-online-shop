using AuditService.Domain;

namespace AuditService.Application;

public sealed class AppendAuditSnapshotUseCase(IAuditSnapshotRepository snapshots)
{
    public async Task<AuditSnapshotDto> ExecuteAsync(CreateAuditSnapshotCommand command, CancellationToken cancellationToken)
    {
        var snapshot = AuditSnapshot.Create(
            command.CorrelationId,
            command.EventType,
            command.Service,
            command.Payload,
            command.PreviousEventId,
            command.Actor,
            command.StatusCode);

        await snapshots.AppendAsync(snapshot, cancellationToken);
        return snapshot.ToDto();
    }
}
