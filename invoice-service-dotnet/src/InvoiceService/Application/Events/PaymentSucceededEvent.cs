using InvoiceService.Domain;
using InvoiceService.Application;

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
    OrderInvoiceDetails? EmbeddedOrderDetails,
    Guid? PreviousEventId,
    string Actor,
    string StatusCode)
{
    public CreateInvoiceCommand ToCommand(OrderInvoiceDetails order, int retryCount)
    {
        var payment = PaymentSucceeded.Create(
            CorrelationId,
            OrderId,
            TransactionId,
            Amount,
            Currency,
            order.CustomerEmail,
            order.Customer,
            order.Items);

        return new CreateInvoiceCommand(payment, retryCount);
    }
}
