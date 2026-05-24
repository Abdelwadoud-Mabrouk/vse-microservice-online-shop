namespace WarehouseService.Application;

public sealed class GetInventoryUseCase(IWarehouseRepository warehouse)
{
    public IReadOnlyList<InventoryItemDto> Execute()
    {
        return warehouse.GetInventory();
    }
}
