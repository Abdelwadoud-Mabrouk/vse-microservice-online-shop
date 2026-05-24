using InvoiceService.Domain;

namespace InvoiceService.Application;

public interface IInvoiceRepository
{
    Task SaveAsync(Invoice invoice, CancellationToken cancellationToken);

    Task<Invoice?> FindAsync(Guid invoiceId, CancellationToken cancellationToken);

    Task<IReadOnlyList<Invoice>> FindByCustomerEmailAsync(string customerEmail, CancellationToken cancellationToken);
}
