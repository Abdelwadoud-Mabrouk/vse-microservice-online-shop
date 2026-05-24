using WarehouseService.Application;
using WarehouseService.Domain;

namespace WarehouseService.Tests;

public sealed class ReserveStockUseCaseTests
{
    private static readonly Guid DeskId = Guid.Parse("22222222-2222-4222-8222-222222222222");

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
                [ReservationItem.Create(DeskId, "SKU-DESK", 1)],
                ReservationStatus.Reserved,
                DateTimeOffset.UtcNow,
                null)));

        var useCase = new ReserveStockUseCase(repository, audit);

        var result = await useCase.ExecuteAsync(new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create(DeskId, "SKU-DESK", 1)]), CancellationToken.None);

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
            [ReservationItem.Create(DeskId, "SKU-DESK", 10)]), CancellationToken.None);

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
        public IReadOnlyList<InventoryItemDto> GetInventory() => [];

        public InventoryItemDto? FindInventoryItem(Guid productId) => null;

        public InventoryChangeResult AddInventoryItem(SaveInventoryItemCommand command) =>
            new(false, "not implemented", null);

        public InventoryChangeResult UpdateInventoryItem(SaveInventoryItemCommand command) =>
            new(false, "not implemented", null);

        public InventoryChangeResult SetStockQuantity(SetStockQuantityCommand command) =>
            new(false, "not implemented", null);

        public InventoryChangeResult RemoveInventoryItem(Guid productId) =>
            new(false, "not implemented", null);

        public ReservationResult TryReserve(ReserveStockCommand command) => result;

        public Reservation? Cancel(Guid reservationId) => null;

        public Reservation? Commit(Guid reservationId) => null;
    }
}
