namespace WarehouseService.Infrastructure.Persistence.Entities;

public sealed class WarehouseReservationItemEntity
{
    public Guid ReservationId { get; set; }

    public Guid ProductId { get; set; }

    public string Sku { get; set; } = string.Empty;

    public int Quantity { get; set; }

    public WarehouseReservationEntity? Reservation { get; set; }
}
