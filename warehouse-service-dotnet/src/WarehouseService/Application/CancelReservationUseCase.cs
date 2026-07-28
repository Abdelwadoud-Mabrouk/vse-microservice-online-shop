using Microsoft.Extensions.Logging.Abstractions;

namespace WarehouseService.Application;

public sealed class CancelReservationUseCase(
    IWarehouseRepository warehouse,
    IAuditSink audit,
    ILogger<CancelReservationUseCase>? logger = null)
{
    private readonly ILogger<CancelReservationUseCase> _logger = logger ?? NullLogger<CancelReservationUseCase>.Instance;

    public async Task<ReservationTransitionResult> ExecuteAsync(Guid reservationId, Guid correlationId, CancellationToken cancellationToken)
    {
        using var scope = _logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "warehouse-service",
            ["correlationId"] = correlationId,
            ["operation"] = "warehouse.cancel_reservation",
            ["entityId"] = reservationId
        });

        var result = warehouse.Cancel(reservationId);
        if (!result.Success)
        {
            _logger.LogWarning(
                "Reservation {ReservationId} could not be cancelled with status {Status} and errorCode {ErrorCode}",
                reservationId,
                "BUSINESS_FAILED",
                "RESERVATION_NOT_FOUND");

            return result;
        }

        _logger.LogInformation(
            "Reservation {ReservationId} cancelled with status {Status}, idempotentReplay {IdempotentReplay}",
            reservationId,
            "COMPENSATED",
            result.Outcome == ReservationTransitionOutcome.AlreadyApplied);

        if (result.Outcome == ReservationTransitionOutcome.Applied)
        {
            await audit.RecordAsync(correlationId, "WAREHOUSE_RESERVATION_CANCELLED", "COMPENSATED", result.Reservation!, cancellationToken);
        }

        return result;
    }
}
