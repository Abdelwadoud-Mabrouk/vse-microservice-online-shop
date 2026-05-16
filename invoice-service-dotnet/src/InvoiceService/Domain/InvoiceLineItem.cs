namespace InvoiceService.Domain;

public sealed record InvoiceLineItem(string Sku, string Name, int Quantity, decimal UnitPrice)
{
    public static InvoiceLineItem Create(string sku, string name, int quantity, decimal unitPrice)
    {
        if (string.IsNullOrWhiteSpace(sku))
        {
            throw new ArgumentException("SKU is required.", nameof(sku));
        }

        if (string.IsNullOrWhiteSpace(name))
        {
            throw new ArgumentException("Item name is required.", nameof(name));
        }

        if (quantity <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(quantity), "Quantity must be positive.");
        }

        if (unitPrice < 0)
        {
            throw new ArgumentOutOfRangeException(nameof(unitPrice), "Unit price must not be negative.");
        }

        return new InvoiceLineItem(sku.Trim(), name.Trim(), quantity, unitPrice);
    }
}
