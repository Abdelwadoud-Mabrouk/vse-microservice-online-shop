namespace InvoiceService.Domain;

public sealed record PaymentSucceeded(
    Guid CorrelationId,
    Guid OrderId,
    string TransactionId,
    decimal Amount,
    string Currency,
    string CustomerEmail,
    CustomerDetails Customer,
    IReadOnlyList<InvoiceLineItem> Items)
{
    public static PaymentSucceeded Create(
        Guid correlationId,
        Guid orderId,
        string transactionId,
        decimal amount,
        string currency,
        string customerEmail,
        CustomerDetails customer,
        IReadOnlyList<InvoiceLineItem> items)
    {
        if (correlationId == Guid.Empty)
        {
            throw new ArgumentException("Correlation id must not be empty.", nameof(correlationId));
        }

        if (orderId == Guid.Empty)
        {
            throw new ArgumentException("Order id must not be empty.", nameof(orderId));
        }

        if (amount <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(amount), "Amount must be positive.");
        }

        if (items.Count == 0)
        {
            throw new ArgumentException("At least one invoice item is required.", nameof(items));
        }

        return new PaymentSucceeded(
            correlationId,
            orderId,
            Required(transactionId, nameof(transactionId)),
            amount,
            Required(currency, nameof(currency)).ToUpperInvariant(),
            Required(customerEmail, nameof(customerEmail)),
            customer,
            items);
    }

    private static string Required(string value, string name)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new ArgumentException($"{name} is required.", name);
        }

        return value.Trim();
    }
}
