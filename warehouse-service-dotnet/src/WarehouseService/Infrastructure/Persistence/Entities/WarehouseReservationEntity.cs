using WarehouseService.Domain;

namespace WarehouseService.Infrastructure.Persistence.Entities;

public sealed class WarehouseReservationEntity
{
    public Guid ReservationId { get; set; }

    public Guid OrderId { get; set; }

    public Guid CorrelationId { get; set; }

    public ReservationStatus Status { get; set; }

    public DateTimeOffset CreatedAtUtc { get; set; }

    public DateTimeOffset? UpdatedAtUtc { get; set; }

    public List<WarehouseReservationItemEntity> Items { get; set; } = [];
}
