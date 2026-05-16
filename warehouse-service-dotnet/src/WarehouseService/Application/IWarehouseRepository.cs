using WarehouseService.Domain;

namespace WarehouseService.Application;

public interface IWarehouseRepository
{
    IReadOnlyDictionary<string, int> GetInventory();

    ReservationResult TryReserve(ReserveStockCommand command);

    Reservation? Cancel(Guid reservationId);

    Reservation? Commit(Guid reservationId);
}
