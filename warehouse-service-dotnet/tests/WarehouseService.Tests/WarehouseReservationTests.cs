using Microsoft.Extensions.Configuration;
using WarehouseService.Application;
using WarehouseService.Domain;
using WarehouseService.Infrastructure;

namespace WarehouseService.Tests;

public sealed class WarehouseReservationTests
{
    [Fact]
    public void TryReserve_WhenStockIsAvailable_CreatesReservationAndReducesStock()
    {
        var repository = NewRepository("SKU-CHAIR:2");
        var command = new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create("SKU-CHAIR", 1)]);

        var result = repository.TryReserve(command);
        var inventory = repository.GetInventory();

        Assert.True(result.Success);
        Assert.NotNull(result.Reservation);
        Assert.Equal(1, inventory["SKU-CHAIR"]);
        Assert.Equal(ReservationStatus.Reserved, result.Reservation.Status);
    }

    [Fact]
    public void TryReserve_WhenStockIsInsufficient_DoesNotChangeInventory()
    {
        var repository = NewRepository("SKU-CHAIR:1");
        var command = new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create("SKU-CHAIR", 2)]);

        var result = repository.TryReserve(command);
        var inventory = repository.GetInventory();

        Assert.False(result.Success);
        Assert.Null(result.Reservation);
        Assert.Equal(1, inventory["SKU-CHAIR"]);
    }

    [Fact]
    public void Cancel_ReturnsReservedStock()
    {
        var repository = NewRepository("SKU-CHAIR:2");
        var reservation = repository.TryReserve(new ReserveStockCommand(
            Guid.NewGuid(),
            Guid.NewGuid(),
            [ReservationItem.Create("SKU-CHAIR", 2)])).Reservation!;

        var cancelled = repository.Cancel(reservation.ReservationId);
        var inventory = repository.GetInventory();

        Assert.NotNull(cancelled);
        Assert.Equal(ReservationStatus.Cancelled, cancelled.Status);
        Assert.Equal(2, inventory["SKU-CHAIR"]);
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
