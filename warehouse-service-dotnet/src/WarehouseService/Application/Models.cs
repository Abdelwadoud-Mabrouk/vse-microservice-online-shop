using WarehouseService.Domain;

namespace WarehouseService.Application;

public sealed record ReserveStockCommand(Guid OrderId, Guid CorrelationId, IReadOnlyList<ReservationItem> Items);

public sealed record ReservationDto(
    Guid ReservationId,
    Guid OrderId,
    Guid CorrelationId,
    IReadOnlyList<ReservationItem> Items,
    ReservationStatus Status,
    DateTimeOffset CreatedAt,
    DateTimeOffset? UpdatedAt);

public sealed record ReservationResult(bool Success, string Message, ReservationDto? Reservation);
