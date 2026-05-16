namespace WarehouseService.Application;

public sealed class ReserveStockUseCase(IWarehouseRepository warehouse, IAuditSink audit)
{
    public async Task<ReservationResult> ExecuteAsync(ReserveStockCommand command, CancellationToken cancellationToken)
    {
        var result = warehouse.TryReserve(command);
        if (!result.Success)
        {
            await audit.RecordAsync(command.CorrelationId, "WAREHOUSE_OUT_OF_STOCK", "FAILURE", new
            {
                command.OrderId,
                result.Message
            }, cancellationToken);

            return result;
        }

        await audit.RecordAsync(command.CorrelationId, "WAREHOUSE_RESERVED", "SUCCESS", result.Reservation!, cancellationToken);
        return result;
    }
}
