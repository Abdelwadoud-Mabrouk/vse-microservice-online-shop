using InvoiceService.Domain;

namespace InvoiceService.Application;

public interface IInvoiceDocumentWriter
{
    Task<string> WriteAsync(Invoice invoice, CancellationToken cancellationToken);
}
