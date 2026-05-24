using Microsoft.Extensions.Logging.Abstractions;

namespace WarehouseService.Application;

public sealed class CancelReservationUseCase(
    IWarehouseRepository warehouse,
    IAuditSink audit,
    ILogger<CancelReservationUseCase>? logger = null)
{
    private readonly ILogger<CancelReservationUseCase> _logger = logger ?? NullLogger<CancelReservationUseCase>.Instance;

    public async Task<ReservationDto?> ExecuteAsync(Guid reservationId, Guid correlationId, CancellationToken cancellationToken)
    {
        using var scope = _logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "warehouse-service",
            ["correlationId"] = correlationId,
            ["operation"] = "warehouse.cancel_reservation",
            ["entityId"] = reservationId
        });

        var reservation = warehouse.Cancel(reservationId);
        if (reservation is null)
        {
            _logger.LogWarning(
                "Reservation {ReservationId} could not be cancelled with status {Status} and errorCode {ErrorCode}",
                reservationId,
                "BUSINESS_FAILED",
                "RESERVATION_NOT_FOUND");

            return null;
        }

        var dto = reservation.ToDto();
        _logger.LogInformation(
            "Reservation {ReservationId} cancelled with status {Status}",
            reservationId,
            "COMPENSATED");

        await audit.RecordAsync(correlationId, "WAREHOUSE_RESERVATION_CANCELLED", "COMPENSATED", dto, cancellationToken);
        return dto;
    }
}
