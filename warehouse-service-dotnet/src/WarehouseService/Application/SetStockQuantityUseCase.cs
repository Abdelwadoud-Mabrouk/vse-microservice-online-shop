namespace WarehouseService.Application;

public sealed class SetStockQuantityUseCase(IWarehouseRepository warehouse, IAuditSink audit)
{
    public async Task<InventoryChangeResult> ExecuteAsync(SetStockQuantityCommand command, CancellationToken cancellationToken)
    {
        var result = warehouse.SetStockQuantity(command);
        await audit.RecordAsync(
            command.CorrelationId,
            result.Success ? "WAREHOUSE_STOCK_CHANGED" : "WAREHOUSE_STOCK_CHANGE_REJECTED",
            result.Success ? "SUCCESS" : "FAILURE",
            new { command.ProductId, command.AvailableQuantity, result.Message },
            cancellationToken);

        return result;
    }
}
