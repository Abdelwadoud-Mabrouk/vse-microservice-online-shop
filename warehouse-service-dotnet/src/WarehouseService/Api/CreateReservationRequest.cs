using WarehouseService.Application;
using WarehouseService.Domain;

namespace WarehouseService.Api;

public sealed record CreateReservationRequest(Guid OrderId, List<ReservationItemRequest>? Items)
{
    public Dictionary<string, string[]>? Validate()
    {
        var errors = new Dictionary<string, string[]>();

        if (OrderId == Guid.Empty)
        {
            errors[nameof(OrderId)] = ["orderId must not be empty."];
        }

        if (Items is null || Items.Count == 0)
        {
            errors[nameof(Items)] = ["At least one item is required."];
        }
        else if (Items.Any(item => string.IsNullOrWhiteSpace(item.Sku) || item.Quantity <= 0))
        {
            errors[nameof(Items)] = ["Each item needs a sku and a positive quantity."];
        }

        return errors.Count == 0 ? null : errors;
    }

    public ReserveStockCommand ToCommand(Guid correlationId)
    {
        var items = Items!
            .Select(item => ReservationItem.Create(item.Sku, item.Quantity))
            .ToArray();

        return new ReserveStockCommand(OrderId, correlationId, items);
    }
}

public sealed record ReservationItemRequest(string Sku, int Quantity);
