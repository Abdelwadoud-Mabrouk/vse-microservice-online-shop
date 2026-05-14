namespace WarehouseService.Application;

public sealed class GetInventoryUseCase(IWarehouseRepository warehouse)
{
    public IReadOnlyDictionary<string, int> Execute()
    {
        return warehouse.GetInventory();
    }
}
