using AuditService.Domain;

namespace AuditService.Application;

public interface IAuditSnapshotRepository
{
    Task AppendAsync(AuditSnapshot snapshot, CancellationToken cancellationToken);

    Task<AuditSnapshot?> FindByIdAsync(Guid eventId, CancellationToken cancellationToken);

    Task<AuditSnapshot?> FindLatestByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken);

    Task<IReadOnlyList<AuditSnapshot>> FindByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken);

    Task<IReadOnlyList<OrderAuditSummaryDto>> ListOrderSummariesAsync(CancellationToken cancellationToken);
}
