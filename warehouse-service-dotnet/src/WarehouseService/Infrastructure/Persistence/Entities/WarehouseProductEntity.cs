namespace WarehouseService.Infrastructure.Persistence.Entities;

public sealed class WarehouseProductEntity
{
    public Guid ProductId { get; set; }

    public string Sku { get; set; } = string.Empty;

    public string Name { get; set; } = string.Empty;

    public string Category { get; set; } = string.Empty;

    public int AvailableQuantity { get; set; }
}
