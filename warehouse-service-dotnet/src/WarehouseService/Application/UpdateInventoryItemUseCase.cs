namespace WarehouseService.Application;

public sealed class UpdateInventoryItemUseCase(IWarehouseRepository warehouse, IAuditSink audit)
{
    public async Task<InventoryChangeResult> ExecuteAsync(SaveInventoryItemCommand command, CancellationToken cancellationToken)
    {
        var result = warehouse.UpdateInventoryItem(command);
        await audit.RecordAsync(
            command.CorrelationId,
            result.Success ? "WAREHOUSE_PRODUCT_UPDATED" : "WAREHOUSE_PRODUCT_UPDATE_REJECTED",
            result.Success ? "SUCCESS" : "FAILURE",
            new { command.ProductId, command.Sku, result.Message },
            cancellationToken);

        return result;
    }
}
