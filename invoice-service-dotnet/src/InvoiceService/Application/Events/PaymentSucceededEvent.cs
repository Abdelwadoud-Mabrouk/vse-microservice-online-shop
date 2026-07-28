using InvoiceService.Domain;

namespace InvoiceService.Application.Events;

public sealed record PaymentSucceededEvent(
    Guid EventId,
    Guid CorrelationId,
    string EventType,
    string Service,
    DateTimeOffset Timestamp,
    Guid OrderId,
    string TransactionId,
    decimal Amount,
    string Currency,
    string CustomerEmail,
    CustomerDetails Customer,
    IReadOnlyList<InvoiceLineItem> Items,
    Guid? PreviousEventId,
    string Actor,
    string StatusCode)
{
    public CreateInvoiceCommand ToCommand(int retryCount)
    {
        var payment = PaymentSucceeded.Create(
            CorrelationId,
            OrderId,
            TransactionId,
            Amount,
            Currency,
            CustomerEmail,
            Customer,
            Items);

        return new CreateInvoiceCommand(payment, retryCount);
    }
}
