using WarehouseService.Application;
using WarehouseService.Domain;

namespace WarehouseService.Tests;

public sealed class ReserveStockUseCaseTests
{
    [Fact]
    public async Task ExecuteAsync_WhenReservationSucceeds_WritesSuccessAuditEvent()
    {
        var audit = new RecordingAuditSink();
        var repository = new StubWarehouseRepository(new ReservationResult(
            true,
            "created",
            new ReservationDto(
                Guid.NewGuid(),
                Guid.NewGuid(),
                Guid.NewGuid(),
                [ReservationItem.Create("SKU-DESK", 1)],
                ReservationStatus.Reserved,
                DateTimeOffset.UtcNow,
                null)));

        var useCase = new ReserveStockUseCase(repository, audit);

        var result = await useCase.ExecuteAsync(new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create("SKU-DESK", 1)]), CancellationToken.None);

        Assert.True(result.Success);
        Assert.Contains(audit.Events, item => item.EventType == "WAREHOUSE_RESERVED" && item.StatusCode == "SUCCESS");
    }

    [Fact]
    public async Task ExecuteAsync_WhenReservationFails_WritesOutOfStockAuditEvent()
    {
        var audit = new RecordingAuditSink();
        var repository = new StubWarehouseRepository(new ReservationResult(false, "out of stock", null));
        var useCase = new ReserveStockUseCase(repository, audit);

        var result = await useCase.ExecuteAsync(new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create("SKU-DESK", 10)]), CancellationToken.None);

        Assert.False(result.Success);
        Assert.Contains(audit.Events, item => item.EventType == "WAREHOUSE_OUT_OF_STOCK" && item.StatusCode == "FAILURE");
    }

    private sealed class RecordingAuditSink : IAuditSink
    {
        public List<(Guid CorrelationId, string EventType, string StatusCode)> Events { get; } = [];

        public Task RecordAsync(Guid correlationId, string eventType, string statusCode, object payload, CancellationToken cancellationToken)
        {
            Events.Add((correlationId, eventType, statusCode));
            return Task.CompletedTask;
        }
    }

    private sealed class StubWarehouseRepository(ReservationResult result) : IWarehouseRepository
    {
        public IReadOnlyDictionary<string, int> GetInventory() => new Dictionary<string, int>();

        public ReservationResult TryReserve(ReserveStockCommand command) => result;

        public Reservation? Cancel(Guid reservationId) => null;

        public Reservation? Commit(Guid reservationId) => null;
    }
}
