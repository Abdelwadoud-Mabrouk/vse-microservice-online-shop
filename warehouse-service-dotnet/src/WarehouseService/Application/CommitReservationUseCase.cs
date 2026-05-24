using Microsoft.Extensions.Logging.Abstractions;

namespace WarehouseService.Application;

public sealed class CommitReservationUseCase(
    IWarehouseRepository warehouse,
    IAuditSink audit,
    ILogger<CommitReservationUseCase>? logger = null)
{
    private readonly ILogger<CommitReservationUseCase> _logger = logger ?? NullLogger<CommitReservationUseCase>.Instance;

    public async Task<ReservationDto?> ExecuteAsync(Guid reservationId, Guid correlationId, CancellationToken cancellationToken)
    {
        using var scope = _logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "warehouse-service",
            ["correlationId"] = correlationId,
            ["operation"] = "warehouse.commit_reservation",
            ["entityId"] = reservationId
        });

        var reservation = warehouse.Commit(reservationId);
        if (reservation is null)
        {
            _logger.LogWarning(
                "Reservation {ReservationId} could not be committed with status {Status} and errorCode {ErrorCode}",
                reservationId,
                "BUSINESS_FAILED",
                "RESERVATION_NOT_FOUND");

            return null;
        }

        var dto = reservation.ToDto();
        _logger.LogInformation(
            "Reservation {ReservationId} committed with status {Status}",
            reservationId,
            "SUCCESS");

        await audit.RecordAsync(correlationId, "WAREHOUSE_COMMITTED", "SUCCESS", dto, cancellationToken);
        return dto;
    }
}
