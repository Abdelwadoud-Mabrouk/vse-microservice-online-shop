namespace WarehouseService.Application;

public sealed class CancelReservationUseCase(IWarehouseRepository warehouse, IAuditSink audit)
{
    public async Task<ReservationDto?> ExecuteAsync(Guid reservationId, Guid correlationId, CancellationToken cancellationToken)
    {
        var reservation = warehouse.Cancel(reservationId);
        if (reservation is null)
        {
            return null;
        }

        var dto = reservation.ToDto();
        await audit.RecordAsync(correlationId, "WAREHOUSE_RESERVATION_CANCELLED", "COMPENSATED", dto, cancellationToken);
        return dto;
    }
}
