using InvoiceService.Domain;

namespace InvoiceService.Application;

public sealed record OrderInvoiceDetails(
    string CustomerEmail,
    CustomerDetails Customer,
    IReadOnlyList<InvoiceLineItem> Items);

public interface IOrderDetailsProvider
{
    Task<OrderInvoiceDetails> GetAsync(Guid orderId, CancellationToken cancellationToken);
}
