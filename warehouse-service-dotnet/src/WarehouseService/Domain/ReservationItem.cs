namespace WarehouseService.Domain;

public sealed record ReservationItem(Guid ProductId, string Sku, int Quantity)
{
    public static ReservationItem Create(Guid productId, string sku, int quantity)
    {
        if (productId == Guid.Empty)
        {
            throw new ArgumentException("Product id must not be empty.", nameof(productId));
        }

        if (string.IsNullOrWhiteSpace(sku))
        {
            throw new ArgumentException("SKU is required.", nameof(sku));
        }

        if (quantity <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(quantity), "Quantity must be positive.");
        }

        return new ReservationItem(productId, sku.Trim(), quantity);
    }
}
