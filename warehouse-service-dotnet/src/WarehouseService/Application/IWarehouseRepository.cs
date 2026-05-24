using WarehouseService.Domain;

namespace WarehouseService.Application;

public interface IWarehouseRepository
{
    IReadOnlyList<InventoryItemDto> GetInventory();

    InventoryItemDto? FindInventoryItem(Guid productId);

    InventoryChangeResult AddInventoryItem(SaveInventoryItemCommand command);

    InventoryChangeResult UpdateInventoryItem(SaveInventoryItemCommand command);

    InventoryChangeResult SetStockQuantity(SetStockQuantityCommand command);

    InventoryChangeResult RemoveInventoryItem(Guid productId);

    ReservationResult TryReserve(ReserveStockCommand command);

    Reservation? Cancel(Guid reservationId);

    Reservation? Commit(Guid reservationId);
}
