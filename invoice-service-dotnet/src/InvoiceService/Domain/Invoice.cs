namespace InvoiceService.Domain;

public sealed record Invoice(
    Guid InvoiceId,
    Guid CorrelationId,
    Guid OrderId,
    string TransactionId,
    decimal Amount,
    string Currency,
    string CustomerEmail,
    CustomerDetails Customer,
    IReadOnlyList<InvoiceLineItem> Items,
    DateTimeOffset CreatedAt,
    string DocumentPath)
{
    public static Invoice FromPayment(PaymentSucceeded payment)
    {
        return new Invoice(
            Guid.NewGuid(),
            payment.CorrelationId,
            payment.OrderId,
            payment.TransactionId,
            payment.Amount,
            payment.Currency,
            payment.CustomerEmail,
            payment.Customer,
            payment.Items,
            DateTimeOffset.UtcNow,
            string.Empty);
    }
}
