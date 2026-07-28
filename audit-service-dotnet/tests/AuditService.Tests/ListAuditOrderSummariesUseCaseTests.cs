using AuditService.Application;
using AuditService.Domain;

namespace AuditService.Tests;

public sealed class ListAuditOrderSummariesUseCaseTests
{
    [Fact]
    public async Task ExecuteAsync_ReturnsLatestStatusPerCorrelationId()
    {
        var firstOrder = Guid.NewGuid();
        var secondOrder = Guid.NewGuid();
        var repository = new InMemoryAuditSnapshotRepository([
            Snapshot(firstOrder, "ORDER_CREATED", "SUCCESS", DateTimeOffset.UtcNow.AddMinutes(-5)),
            Snapshot(firstOrder, "ORDER_COMPLETED", "SUCCESS", DateTimeOffset.UtcNow.AddMinutes(-1)),
            Snapshot(secondOrder, "WAREHOUSE_COMMIT_FAILED", "FAILURE", DateTimeOffset.UtcNow.AddMinutes(-2))
        ]);

        var useCase = new ListAuditOrderSummariesUseCase(repository);

        var summaries = await useCase.ExecuteAsync(CancellationToken.None);

        var firstSummary = Assert.Single(summaries, summary => summary.CorrelationId == firstOrder);
        Assert.Equal("ORDER_COMPLETED", firstSummary.LatestEventType);
        Assert.Equal(2, firstSummary.EventCount);

        var secondSummary = Assert.Single(summaries, summary => summary.CorrelationId == secondOrder);
        Assert.Equal("WAREHOUSE_COMMIT_FAILED", secondSummary.LatestEventType);
        Assert.Equal("FAILURE", secondSummary.LatestStatusCode);
    }

    private static AuditSnapshot Snapshot(Guid correlationId, string eventType, string statusCode, DateTimeOffset timestamp)
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
            statusCode);
    }

    private sealed class InMemoryAuditSnapshotRepository(IReadOnlyList<AuditSnapshot> snapshots) : IAuditSnapshotRepository
    {
        public Task AppendAsync(AuditSnapshot snapshot, CancellationToken cancellationToken)
        {
            throw new NotSupportedException();
        }

        public Task<AuditSnapshot?> FindByIdAsync(Guid eventId, CancellationToken cancellationToken) =>
            Task.FromResult(snapshots.SingleOrDefault(snapshot => snapshot.Id == eventId));

        public Task<AuditSnapshot?> FindLatestByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken) =>
            Task.FromResult(snapshots
                .Where(snapshot => snapshot.CorrelationId == correlationId)
                .OrderByDescending(snapshot => snapshot.Timestamp)
                .FirstOrDefault());

        public Task<IReadOnlyList<AuditSnapshot>> FindByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken)
        {
            throw new NotSupportedException();
        }

        public Task<IReadOnlyList<OrderAuditSummaryDto>> ListOrderSummariesAsync(CancellationToken cancellationToken)
        {
            IReadOnlyList<OrderAuditSummaryDto> result = snapshots
                .GroupBy(snapshot => snapshot.CorrelationId)
                .Select(group =>
                {
                    var latest = group.OrderByDescending(snapshot => snapshot.Timestamp).First();
                    return new OrderAuditSummaryDto(
                        latest.CorrelationId,
                        latest.EventType,
                        latest.StatusCode,
                        latest.Service,
                        latest.Timestamp,
                        group.Count());
                })
                .OrderByDescending(summary => summary.LastUpdatedAt)
                .ToArray();

            return Task.FromResult(result);
        }
    }
}
