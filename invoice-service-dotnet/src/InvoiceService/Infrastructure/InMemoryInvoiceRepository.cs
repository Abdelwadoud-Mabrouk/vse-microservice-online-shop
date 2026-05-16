using System.Collections.Concurrent;
using InvoiceService.Application;
using InvoiceService.Domain;

namespace InvoiceService.Infrastructure;

public sealed class InMemoryInvoiceRepository : IInvoiceRepository
{
    private readonly ConcurrentDictionary<Guid, Invoice> _invoices = new();

    public Task SaveAsync(Invoice invoice, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        _invoices[invoice.InvoiceId] = invoice;
        return Task.CompletedTask;
    }

    public Task<Invoice?> FindAsync(Guid invoiceId, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        _invoices.TryGetValue(invoiceId, out var invoice);
        return Task.FromResult(invoice);
    }
}
