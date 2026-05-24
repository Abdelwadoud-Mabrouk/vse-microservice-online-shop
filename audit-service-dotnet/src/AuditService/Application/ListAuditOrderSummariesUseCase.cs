namespace AuditService.Application;

public sealed class ListAuditOrderSummariesUseCase(IAuditSnapshotRepository snapshots)
{
    public Task<IReadOnlyList<OrderAuditSummaryDto>> ExecuteAsync(CancellationToken cancellationToken)
    {
        return snapshots.ListOrderSummariesAsync(cancellationToken);
    }
}
