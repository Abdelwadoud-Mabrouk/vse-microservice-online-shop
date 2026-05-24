using WarehouseService.Application;

namespace WarehouseService.Api;

public sealed record SaveInventoryItemRequest(
    Guid ProductId,
    string? Sku,
    string? Name,
    string? Category,
    int AvailableQuantity)
{
    public Dictionary<string, string[]>? Validate(bool requireProductId)
    {
        var errors = new Dictionary<string, string[]>();

        if (requireProductId && ProductId == Guid.Empty)
        {
            errors[nameof(ProductId)] = ["productId must not be empty."];
        }

        if (string.IsNullOrWhiteSpace(Sku))
        {
            errors[nameof(Sku)] = ["sku is required."];
        }

        if (string.IsNullOrWhiteSpace(Name))
        {
            errors[nameof(Name)] = ["name is required."];
        }

        if (string.IsNullOrWhiteSpace(Category))
        {
            errors[nameof(Category)] = ["category is required."];
        }

        if (AvailableQuantity < 0)
        {
            errors[nameof(AvailableQuantity)] = ["availableQuantity must not be negative."];
        }

        return errors.Count == 0 ? null : errors;
    }

    public SaveInventoryItemCommand ToCommand(Guid productId, Guid correlationId)
    {
        return new SaveInventoryItemCommand(
            productId,
            correlationId,
            Sku!.Trim(),
            Name!.Trim(),
            Category!.Trim(),
            AvailableQuantity);
    }
}

public sealed record SetStockQuantityRequest(int AvailableQuantity)
{
    public Dictionary<string, string[]>? Validate()
    {
        if (AvailableQuantity >= 0)
        {
            return null;
        }

        return new Dictionary<string, string[]>
        {
            [nameof(AvailableQuantity)] = ["availableQuantity must not be negative."]
        };
    }

    public SetStockQuantityCommand ToCommand(Guid productId, Guid correlationId)
    {
        return new SetStockQuantityCommand(productId, correlationId, AvailableQuantity);
    }
}
