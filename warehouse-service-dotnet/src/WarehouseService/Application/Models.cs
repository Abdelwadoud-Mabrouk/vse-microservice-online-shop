using WarehouseService.Domain;

namespace WarehouseService.Application;

public sealed record ReserveStockCommand(Guid OrderId, Guid CorrelationId, IReadOnlyList<ReservationItem> Items);

public sealed record InventoryItemDto(Guid ProductId, string Sku, string Name, string Category, int AvailableQuantity);

public sealed record SaveInventoryItemCommand(
    Guid ProductId,
    Guid CorrelationId,
    string Sku,
    string Name,
    string Category,
    int AvailableQuantity);

public sealed record SetStockQuantityCommand(Guid ProductId, Guid CorrelationId, int AvailableQuantity);

public sealed record InventoryChangeResult(bool Success, string Message, InventoryItemDto? Item);

public sealed record ReservationDto(
    Guid ReservationId,
    Guid OrderId,
    Guid CorrelationId,
    IReadOnlyList<ReservationItem> Items,
    ReservationStatus Status,
    DateTimeOffset CreatedAt,
    DateTimeOffset? UpdatedAt);

public sealed record ReservationResult(bool Success, string Message, ReservationDto? Reservation);
