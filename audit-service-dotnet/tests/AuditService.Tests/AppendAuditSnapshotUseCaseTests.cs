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

    [Fact]
    public async Task ExecuteAsync_WhenEventIdIsRepeated_DoesNotAppendSecondSnapshot()
    {
        var repository = new InMemoryAuditSnapshotRepository();
        var useCase = new AppendAuditSnapshotUseCase(repository);
        var eventId = Guid.NewGuid();
        var command = new CreateAuditSnapshotCommand(
            Guid.NewGuid(), "ORDER_COMPLETED", "shop-service", new Dictionary<string, object?>(),
            null, "shop-service", "SUCCESS", eventId);

        var first = await useCase.ExecuteAsync(command, CancellationToken.None);
        var second = await useCase.ExecuteAsync(command, CancellationToken.None);

        Assert.Equal(first.Id, second.Id);
        Assert.Single(repository.Snapshots);
    }

    [Fact]
    public async Task ExecuteAsync_WhenPreviousEventIdIsMissing_LinksToLatestSnapshot()
    {
        var repository = new InMemoryAuditSnapshotRepository();
        var useCase = new AppendAuditSnapshotUseCase(repository);
        var correlationId = Guid.NewGuid();

        var first = await useCase.ExecuteAsync(new CreateAuditSnapshotCommand(
            correlationId, "ORDER_PLACED", "shop-service", new Dictionary<string, object?>(),
            null, "shop-service", "SUCCESS"), CancellationToken.None);
        var second = await useCase.ExecuteAsync(new CreateAuditSnapshotCommand(
            correlationId, "WAREHOUSE_RESERVED", "warehouse-service", new Dictionary<string, object?>(),
            null, "warehouse-service", "SUCCESS"), CancellationToken.None);

        Assert.Equal(first.Id, second.PreviousEventId);
    }

    private sealed class InMemoryAuditSnapshotRepository : IAuditSnapshotRepository
    {
        public List<AuditSnapshot> Snapshots { get; } = [];

        public Task AppendAsync(AuditSnapshot snapshot, CancellationToken cancellationToken)
        {
            Snapshots.Add(snapshot);
            return Task.CompletedTask;
        }

        public Task<AuditSnapshot?> FindByIdAsync(Guid eventId, CancellationToken cancellationToken) =>
            Task.FromResult(Snapshots.SingleOrDefault(snapshot => snapshot.Id == eventId));

        public Task<AuditSnapshot?> FindLatestByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken) =>
            Task.FromResult(Snapshots
                .Where(snapshot => snapshot.CorrelationId == correlationId)
                .OrderByDescending(snapshot => snapshot.Timestamp)
                .ThenByDescending(snapshot => snapshot.Id)
                .FirstOrDefault());

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
