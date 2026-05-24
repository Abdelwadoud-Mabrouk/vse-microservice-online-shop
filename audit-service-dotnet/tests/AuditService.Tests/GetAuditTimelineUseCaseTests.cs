using AuditService.Application;
using AuditService.Domain;

namespace AuditService.Tests;

public sealed class GetAuditTimelineUseCaseTests
{
    [Fact]
    public async Task ExecuteAsync_ReturnsOnlySnapshotsForCorrelationInTimelineOrder()
    {
        var correlationId = Guid.NewGuid();
        var otherCorrelationId = Guid.NewGuid();
        var repository = new InMemoryAuditSnapshotRepository([
            Snapshot(otherCorrelationId, "OTHER", DateTimeOffset.UtcNow.AddMinutes(-3)),
            Snapshot(correlationId, "PAYMENT_SUCCEEDED", DateTimeOffset.UtcNow.AddMinutes(2)),
            Snapshot(correlationId, "ORDER_PLACED", DateTimeOffset.UtcNow.AddMinutes(-2))
        ]);

        var useCase = new GetAuditTimelineUseCase(repository);

        var timeline = await useCase.ExecuteAsync(correlationId, CancellationToken.None);

        Assert.Equal(["ORDER_PLACED", "PAYMENT_SUCCEEDED"], timeline.Select(item => item.EventType).ToArray());
    }

    private static AuditSnapshot Snapshot(Guid correlationId, string eventType, DateTimeOffset timestamp)
    {
        return new AuditSnapshot(
            Guid.NewGuid(),
            correlationId,
            eventType,
            "test-service",
            timestamp,
            new Dictionary<string, object?>(),
            null,
            "test",
            "SUCCESS");
    }

    private sealed class InMemoryAuditSnapshotRepository(IReadOnlyList<AuditSnapshot> snapshots) : IAuditSnapshotRepository
    {
        public Task AppendAsync(AuditSnapshot snapshot, CancellationToken cancellationToken)
        {
            throw new NotSupportedException();
        }

        public Task<IReadOnlyList<AuditSnapshot>> FindByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken)
        {
            IReadOnlyList<AuditSnapshot> result = snapshots
                .Where(snapshot => snapshot.CorrelationId == correlationId)
                .OrderBy(snapshot => snapshot.Timestamp)
                .ToArray();

            return Task.FromResult(result);
        }

        public Task<IReadOnlyList<OrderAuditSummaryDto>> ListOrderSummariesAsync(CancellationToken cancellationToken)
        {
            throw new NotSupportedException();
        }
    }
}
