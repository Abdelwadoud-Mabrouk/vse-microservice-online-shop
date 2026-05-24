namespace WarehouseService.Application;

public sealed class RemoveInventoryItemUseCase(IWarehouseRepository warehouse, IAuditSink audit)
{
    public async Task<InventoryChangeResult> ExecuteAsync(Guid productId, Guid correlationId, CancellationToken cancellationToken)
    {
        var result = warehouse.RemoveInventoryItem(productId);
        await audit.RecordAsync(
            correlationId,
            result.Success ? "WAREHOUSE_PRODUCT_REMOVED" : "WAREHOUSE_PRODUCT_REMOVE_REJECTED",
            result.Success ? "SUCCESS" : "FAILURE",
            new { ProductId = productId, result.Message },
            cancellationToken);

        return result;
    }
}
