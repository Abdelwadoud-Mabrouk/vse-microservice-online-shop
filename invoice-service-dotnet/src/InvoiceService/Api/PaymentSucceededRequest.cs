using InvoiceService.Application;
using InvoiceService.Domain;

namespace InvoiceService.Api;

public sealed record PaymentSucceededRequest(
    Guid CorrelationId,
    Guid OrderId,
    string? TransactionId,
    decimal Amount,
    string? Currency,
    string? CustomerEmail,
    List<InvoiceLineItemRequest>? Items)
{
    public Dictionary<string, string[]>? Validate()
    {
        var errors = new Dictionary<string, string[]>();

        if (OrderId == Guid.Empty)
        {
            errors[nameof(OrderId)] = ["orderId must not be empty."];
        }

        if (string.IsNullOrWhiteSpace(TransactionId))
        {
            errors[nameof(TransactionId)] = ["transactionId is required."];
        }

        if (Amount <= 0)
        {
            errors[nameof(Amount)] = ["amount must be positive."];
        }

        if (string.IsNullOrWhiteSpace(Currency))
        {
            errors[nameof(Currency)] = ["currency is required."];
        }

        if (string.IsNullOrWhiteSpace(CustomerEmail))
        {
            errors[nameof(CustomerEmail)] = ["customerEmail is required."];
        }

        if (Items is null || Items.Count == 0)
        {
            errors[nameof(Items)] = ["At least one invoice item is required."];
        }
        else if (Items.Any(item => string.IsNullOrWhiteSpace(item.Sku) || string.IsNullOrWhiteSpace(item.Name) || item.Quantity <= 0 || item.UnitPrice < 0))
        {
            errors[nameof(Items)] = ["Each item needs sku, name, positive quantity, and non-negative unitPrice."];
        }

        return errors.Count == 0 ? null : errors;
    }

    public CreateInvoiceCommand ToCommand(Guid correlationId, int retryCount)
    {
        var items = Items!
            .Select(item => InvoiceLineItem.Create(item.Sku!, item.Name!, item.Quantity, item.UnitPrice))
            .ToArray();

        var payment = PaymentSucceeded.Create(
            correlationId,
            OrderId,
            TransactionId!,
            Amount,
            Currency!,
            CustomerEmail!,
            items);

        return new CreateInvoiceCommand(payment, retryCount);
    }
}

public sealed record InvoiceLineItemRequest(string? Sku, string? Name, int Quantity, decimal UnitPrice);
