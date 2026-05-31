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

    [Fact]
    public async Task CommitAsync_WhenReservationExists_WritesCommittedAuditEvent()
    {
        var audit = new RecordingAuditSink();
        var reservation = NewReservation().Commit();
        var repository = new StubWarehouseRepository(
            new ReservationResult(true, "created", null),
            committedReservation: reservation);

        var useCase = new CommitReservationUseCase(repository, audit);

        var result = await useCase.ExecuteAsync(reservation.ReservationId, reservation.CorrelationId, CancellationToken.None);

        Assert.NotNull(result);
        Assert.Equal(ReservationStatus.Committed, result.Status);
        Assert.Contains(audit.Events, item => item.EventType == "WAREHOUSE_COMMITTED" && item.StatusCode == "SUCCESS");
    }

    [Fact]
    public async Task CommitAsync_WhenReservationIsMissing_DoesNotWriteSuccessAuditEvent()
    {
        var audit = new RecordingAuditSink();
        var repository = new StubWarehouseRepository(new ReservationResult(false, "missing", null));
        var useCase = new CommitReservationUseCase(repository, audit);

        var result = await useCase.ExecuteAsync(Guid.NewGuid(), Guid.NewGuid(), CancellationToken.None);

        Assert.Null(result);
        Assert.DoesNotContain(audit.Events, item => item.EventType == "WAREHOUSE_COMMITTED");
    }

    [Fact]
    public async Task CancelAsync_WhenReservationExists_WritesCompensationAuditEvent()
    {
        var audit = new RecordingAuditSink();
        var reservation = NewReservation().Cancel();
        var repository = new StubWarehouseRepository(
            new ReservationResult(true, "created", null),
            cancelledReservation: reservation);

        var useCase = new CancelReservationUseCase(repository, audit);

        var result = await useCase.ExecuteAsync(reservation.ReservationId, reservation.CorrelationId, CancellationToken.None);

        Assert.NotNull(result);
        Assert.Equal(ReservationStatus.Cancelled, result.Status);
        Assert.Contains(audit.Events, item => item.EventType == "WAREHOUSE_RESERVATION_CANCELLED" && item.StatusCode == "COMPENSATED");
    }

    private static Reservation NewReservation()
    {
        return Reservation.Create(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create(DeskId, "SKU-DESK", 1)]);
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

    private sealed class StubWarehouseRepository(
        ReservationResult result,
        Reservation? cancelledReservation = null,
        Reservation? committedReservation = null) : IWarehouseRepository
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

        public Reservation? Cancel(Guid reservationId) => cancelledReservation;

        public Reservation? Commit(Guid reservationId) => committedReservation;
    }
}
