namespace AuditService.Application;

public sealed class GetAuditTimelineUseCase(IAuditSnapshotRepository snapshots)
{
    public async Task<IReadOnlyList<AuditSnapshotDto>> ExecuteAsync(Guid correlationId, CancellationToken cancellationToken)
    {
        var timeline = await snapshots.FindByCorrelationIdAsync(correlationId, cancellationToken);
        return timeline.Select(snapshot => snapshot.ToDto()).ToArray();
    }
}
