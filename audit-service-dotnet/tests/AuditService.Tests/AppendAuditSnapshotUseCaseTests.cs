using AuditService.Application;
using AuditService.Domain;

namespace AuditService.Tests;

public sealed class AppendAuditSnapshotUseCaseTests
{
    [Fact]
    public async Task ExecuteAsync_StoresSnapshotAndReturnsCreatedDto()
    {
        var repository = new InMemoryAuditSnapshotRepository();
        var useCase = new AppendAuditSnapshotUseCase(repository);
        var correlationId = Guid.NewGuid();

        var dto = await useCase.ExecuteAsync(new CreateAuditSnapshotCommand(
            correlationId,
            " WAREHOUSE_RESERVED ",
            " warehouse-service ",
            new Dictionary<string, object?> { ["orderId"] = Guid.NewGuid() },
            null,
            "warehouse-service",
            "SUCCESS"), CancellationToken.None);

        Assert.NotEqual(Guid.Empty, dto.Id);
        Assert.Equal(correlationId, dto.CorrelationId);
        Assert.Equal("WAREHOUSE_RESERVED", dto.EventType);
        Assert.Equal("warehouse-service", dto.Service);
        Assert.Single(repository.Snapshots);
    }

    [Fact]
    public async Task ExecuteAsync_RejectsEmptyCorrelationId()
    {
        var repository = new InMemoryAuditSnapshotRepository();
        var useCase = new AppendAuditSnapshotUseCase(repository);

        await Assert.ThrowsAsync<ArgumentException>(() => useCase.ExecuteAsync(new CreateAuditSnapshotCommand(
            Guid.Empty,
            "ORDER_PLACED",
            "shop-service",
            new Dictionary<string, object?>(),
            null,
            "shop-service",
            "SUCCESS"), CancellationToken.None));
    }

    private sealed class InMemoryAuditSnapshotRepository : IAuditSnapshotRepository
    {
        public List<AuditSnapshot> Snapshots { get; } = [];

        public Task AppendAsync(AuditSnapshot snapshot, CancellationToken cancellationToken)
        {
            Snapshots.Add(snapshot);
            return Task.CompletedTask;
        }

        public Task<IReadOnlyList<AuditSnapshot>> FindByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken)
        {
            IReadOnlyList<AuditSnapshot> result = Snapshots
                .Where(snapshot => snapshot.CorrelationId == correlationId)
                .OrderBy(snapshot => snapshot.Timestamp)
                .ToArray();

            return Task.FromResult(result);
        }

        public Task<IReadOnlyList<OrderAuditSummaryDto>> ListOrderSummariesAsync(CancellationToken cancellationToken)
        {
            IReadOnlyList<OrderAuditSummaryDto> result = Snapshots
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
                .ToArray();

            return Task.FromResult(result);
        }
    }
}
