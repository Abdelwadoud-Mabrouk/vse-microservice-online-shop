using Microsoft.Extensions.Configuration;
using WarehouseService.Application;
using WarehouseService.Domain;
using WarehouseService.Infrastructure;

namespace WarehouseService.Tests;

public sealed class WarehouseReservationTests
{
    private static readonly Guid ChairId = Guid.Parse("11111111-1111-4111-8111-111111111111");

    [Fact]
    public void TryReserve_WhenStockIsAvailable_CreatesReservationAndReducesStock()
    {
        var repository = NewRepository($"{ChairId}:SKU-CHAIR:2");
        var command = new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create(ChairId, "SKU-CHAIR", 1)]);

        var result = repository.TryReserve(command);
        var inventory = repository.GetInventory();

        Assert.True(result.Success);
        Assert.NotNull(result.Reservation);
        Assert.Equal(1, inventory.Single(item => item.ProductId == ChairId).AvailableQuantity);
        Assert.Equal(ReservationStatus.Reserved, result.Reservation.Status);
    }

    [Fact]
    public void TryReserve_WhenStockIsInsufficient_DoesNotChangeInventory()
    {
        var repository = NewRepository($"{ChairId}:SKU-CHAIR:1");
        var command = new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create(ChairId, "SKU-CHAIR", 2)]);

        var result = repository.TryReserve(command);
        var inventory = repository.GetInventory();

        Assert.False(result.Success);
        Assert.Null(result.Reservation);
        Assert.Equal(1, inventory.Single(item => item.ProductId == ChairId).AvailableQuantity);
    }

    [Fact]
    public void Cancel_ReturnsReservedStock()
    {
        var repository = NewRepository($"{ChairId}:SKU-CHAIR:2");
        var reservation = repository.TryReserve(new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create(ChairId, "SKU-CHAIR", 2)])).Reservation!;

        var cancelled = repository.Cancel(reservation.ReservationId);
        var inventory = repository.GetInventory();

        Assert.True(cancelled.Success);
        Assert.Equal(ReservationStatus.Cancelled, cancelled.Reservation!.Status);
        Assert.Equal(2, inventory.Single(item => item.ProductId == ChairId).AvailableQuantity);
    }

    [Fact]
    public void TryReserve_WhenSameOrderIsRepeated_ReturnsSameReservationWithoutReducingStockTwice()
    {
        var repository = NewRepository($"{ChairId}:SKU-CHAIR:2");
        var command = new ReserveStockCommand(
            Guid.NewGuid(), Guid.NewGuid(), [ReservationItem.Create(ChairId, "SKU-CHAIR", 1)]);

        var first = repository.TryReserve(command);
        var second = repository.TryReserve(command);

        Assert.True(second.IsReplay);
        Assert.Equal(first.Reservation!.ReservationId, second.Reservation!.ReservationId);
        Assert.Equal(1, repository.GetInventory().Single().AvailableQuantity);
    }

    [Fact]
    public void Commit_WhenRepeated_IsIdempotent()
    {
        var repository = NewRepository($"{ChairId}:SKU-CHAIR:1");
        var reservation = repository.TryReserve(new ReserveStockCommand(
            Guid.NewGuid(), Guid.NewGuid(), [ReservationItem.Create(ChairId, "SKU-CHAIR", 1)])).Reservation!;

        var first = repository.Commit(reservation.ReservationId);
        var second = repository.Commit(reservation.ReservationId);

        Assert.Equal(ReservationTransitionOutcome.Applied, first.Outcome);
        Assert.Equal(ReservationTransitionOutcome.AlreadyApplied, second.Outcome);
        Assert.Equal(0, repository.GetInventory().Single().AvailableQuantity);
    }

    [Fact]
    public void Cancel_WhenRepeated_ReturnsStockOnlyOnce()
    {
        var repository = NewRepository($"{ChairId}:SKU-CHAIR:1");
        var reservation = repository.TryReserve(new ReserveStockCommand(
            Guid.NewGuid(), Guid.NewGuid(), [ReservationItem.Create(ChairId, "SKU-CHAIR", 1)])).Reservation!;

        var first = repository.Cancel(reservation.ReservationId);
        var second = repository.Cancel(reservation.ReservationId);

        Assert.Equal(ReservationTransitionOutcome.Applied, first.Outcome);
        Assert.Equal(ReservationTransitionOutcome.AlreadyApplied, second.Outcome);
        Assert.Equal(1, repository.GetInventory().Single().AvailableQuantity);
    }

    [Fact]
    public void Commit_AfterCancel_ReturnsConflict()
    {
        var repository = NewRepository($"{ChairId}:SKU-CHAIR:1");
        var reservation = repository.TryReserve(new ReserveStockCommand(
            Guid.NewGuid(), Guid.NewGuid(), [ReservationItem.Create(ChairId, "SKU-CHAIR", 1)])).Reservation!;
        repository.Cancel(reservation.ReservationId);

        Assert.Equal(ReservationTransitionOutcome.Conflict, repository.Commit(reservation.ReservationId).Outcome);
    }

    [Fact]
    public void Cancel_AfterCommit_ReturnsConflict()
    {
        var repository = NewRepository($"{ChairId}:SKU-CHAIR:1");
        var reservation = repository.TryReserve(new ReserveStockCommand(
            Guid.NewGuid(), Guid.NewGuid(), [ReservationItem.Create(ChairId, "SKU-CHAIR", 1)])).Reservation!;
        repository.Commit(reservation.ReservationId);

        Assert.Equal(ReservationTransitionOutcome.Conflict, repository.Cancel(reservation.ReservationId).Outcome);
    }

    private static InMemoryWarehouseRepository NewRepository(string stock)
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["WAREHOUSE_INITIAL_STOCK"] = stock
            })
            .Build();

        return new InMemoryWarehouseRepository(configuration);
    }
}
