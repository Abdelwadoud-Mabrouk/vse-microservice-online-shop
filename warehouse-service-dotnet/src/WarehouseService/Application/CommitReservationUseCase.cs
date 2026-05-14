namespace WarehouseService.Application;

public sealed class CommitReservationUseCase(IWarehouseRepository warehouse, IAuditSink audit)
{
    public async Task<ReservationDto?> ExecuteAsync(Guid reservationId, Guid correlationId, CancellationToken cancellationToken)
    {
        var reservation = warehouse.Commit(reservationId);
        if (reservation is null)
        {
            return null;
        }

        var dto = reservation.ToDto();
        await audit.RecordAsync(correlationId, "WAREHOUSE_COMMITTED", "SUCCESS", dto, cancellationToken);
        return dto;
    }
}
