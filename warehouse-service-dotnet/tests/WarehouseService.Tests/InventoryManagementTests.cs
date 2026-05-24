using Microsoft.Extensions.Configuration;
using WarehouseService.Application;
using WarehouseService.Infrastructure;

namespace WarehouseService.Tests;

public sealed class InventoryManagementTests
{
    private static readonly Guid ExistingProductId = Guid.Parse("11111111-1111-4111-8111-111111111111");
    private static readonly Guid NewProductId = Guid.Parse("aaaaaaaa-0000-4000-8000-000000000001");

    [Fact]
    public void AddInventoryItem_WhenProductIsNew_AddsProduct()
    {
        var repository = NewRepository();

        var result = repository.AddInventoryItem(new SaveInventoryItemCommand(
            NewProductId,
            Guid.NewGuid(),
            "SKU-TEST",
            "Test Stuhl",
            "Stuehle",
            4));

        Assert.True(result.Success);
        Assert.Equal(4, repository.FindInventoryItem(NewProductId)!.AvailableQuantity);
    }

    [Fact]
    public void SetStockQuantity_WhenProductExists_ChangesAvailableQuantity()
    {
        var repository = NewRepository();

        var result = repository.SetStockQuantity(new SetStockQuantityCommand(
            ExistingProductId,
            Guid.NewGuid(),
            42));

        Assert.True(result.Success);
        Assert.Equal(42, repository.FindInventoryItem(ExistingProductId)!.AvailableQuantity);
    }

    [Fact]
    public void RemoveInventoryItem_WhenProductExists_RemovesProduct()
    {
        var repository = NewRepository();

        var result = repository.RemoveInventoryItem(ExistingProductId);

        Assert.True(result.Success);
        Assert.Null(repository.FindInventoryItem(ExistingProductId));
    }

    private static InMemoryWarehouseRepository NewRepository()
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["WAREHOUSE_INITIAL_STOCK"] = $"{ExistingProductId}:SKU-CHAIR:2"
            })
            .Build();

        return new InMemoryWarehouseRepository(configuration);
    }
}
