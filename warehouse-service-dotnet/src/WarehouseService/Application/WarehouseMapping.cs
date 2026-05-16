using WarehouseService.Domain;

namespace WarehouseService.Application;

internal static class WarehouseMapping
{
    public static ReservationDto ToDto(this Reservation reservation)
    {
        return new ReservationDto(
            reservation.ReservationId,
            reservation.OrderId,
            reservation.CorrelationId,
            reservation.Items,
            reservation.Status,
            reservation.CreatedAt,
            reservation.UpdatedAt);
    }
}
