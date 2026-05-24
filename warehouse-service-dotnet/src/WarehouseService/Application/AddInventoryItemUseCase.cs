namespace WarehouseService.Application;

public sealed class AddInventoryItemUseCase(IWarehouseRepository warehouse, IAuditSink audit)
{
    public async Task<InventoryChangeResult> ExecuteAsync(SaveInventoryItemCommand command, CancellationToken cancellationToken)
    {
        var result = warehouse.AddInventoryItem(command);
        await audit.RecordAsync(
            command.CorrelationId,
            result.Success ? "WAREHOUSE_PRODUCT_CREATED" : "WAREHOUSE_PRODUCT_CREATE_REJECTED",
            result.Success ? "SUCCESS" : "FAILURE",
            new { command.ProductId, command.Sku, result.Message },
            cancellationToken);

        return result;
    }
}
