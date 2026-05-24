using AuditService.Domain;

namespace AuditService.Application;

public interface IAuditSnapshotRepository
{
    Task AppendAsync(AuditSnapshot snapshot, CancellationToken cancellationToken);

    Task<IReadOnlyList<AuditSnapshot>> FindByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken);

    Task<IReadOnlyList<OrderAuditSummaryDto>> ListOrderSummariesAsync(CancellationToken cancellationToken);
}
