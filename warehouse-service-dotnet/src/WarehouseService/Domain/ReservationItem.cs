namespace WarehouseService.Domain;

public sealed record ReservationItem(string Sku, int Quantity)
{
    public static ReservationItem Create(string sku, int quantity)
    {
        if (string.IsNullOrWhiteSpace(sku))
        {
            throw new ArgumentException("SKU is required.", nameof(sku));
        }

        if (quantity <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(quantity), "Quantity must be positive.");
        }

        return new ReservationItem(sku.Trim(), quantity);
    }
}
