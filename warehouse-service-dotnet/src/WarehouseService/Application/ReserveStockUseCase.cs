using Microsoft.Extensions.Logging.Abstractions;

namespace WarehouseService.Application;

public sealed class ReserveStockUseCase(
    IWarehouseRepository warehouse,
    IAuditSink audit,
    ILogger<ReserveStockUseCase>? logger = null)
{
    private readonly ILogger<ReserveStockUseCase> _logger = logger ?? NullLogger<ReserveStockUseCase>.Instance;

    public async Task<ReservationResult> ExecuteAsync(ReserveStockCommand command, CancellationToken cancellationToken)
    {
        using var scope = _logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "warehouse-service",
            ["correlationId"] = command.CorrelationId,
            ["operation"] = "warehouse.reserve_stock",
            ["orderId"] = command.OrderId
        });

        var result = warehouse.TryReserve(command);
        if (!result.Success)
        {
            _logger.LogWarning(
                "Stock reservation failed for order {OrderId} with status {Status}, errorCode {ErrorCode}, reason {Reason}",
                command.OrderId,
                "BUSINESS_FAILED",
                "OUT_OF_STOCK",
                result.Message);

            await audit.RecordAsync(command.CorrelationId, "WAREHOUSE_OUT_OF_STOCK", "FAILURE", new
            {
                command.OrderId,
                result.Message
            }, cancellationToken);

            return result;
        }

        if (result.IsReplay)
        {
            _logger.LogInformation(
                "Existing reservation {ReservationId} returned for order {OrderId} because an idempotent retry was detected",
                result.Reservation!.ReservationId,
                command.OrderId);
            return result;
        }

        _logger.LogInformation(
            "Stock reserved for order {OrderId} with reservation {ReservationId} and status {Status}",
            command.OrderId,
            result.Reservation!.ReservationId,
            "SUCCESS");

        await audit.RecordAsync(command.CorrelationId, "WAREHOUSE_RESERVED", "SUCCESS", result.Reservation!, cancellationToken);
        return result;
    }
}
