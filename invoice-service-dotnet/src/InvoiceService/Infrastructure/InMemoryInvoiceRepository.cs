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

    public Task<Invoice?> FindByTransactionIdAsync(string transactionId, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        return Task.FromResult(_invoices.Values.SingleOrDefault(invoice =>
            string.Equals(invoice.TransactionId, transactionId, StringComparison.Ordinal)));
    }

    public Task<IReadOnlyList<Invoice>> FindByCustomerEmailAsync(string customerEmail, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        return Task.FromResult<IReadOnlyList<Invoice>>(
            _invoices.Values
                .Where(invoice => string.Equals(invoice.CustomerEmail, customerEmail, StringComparison.OrdinalIgnoreCase))
                .ToArray());
    }
}
