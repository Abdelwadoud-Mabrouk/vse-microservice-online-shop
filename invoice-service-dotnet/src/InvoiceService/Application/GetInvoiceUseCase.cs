namespace InvoiceService.Application;

public sealed class GetInvoiceUseCase(IInvoiceRepository invoices)
{
    public async Task<InvoiceDto?> ExecuteAsync(Guid invoiceId, CancellationToken cancellationToken)
    {
        var invoice = await invoices.FindAsync(invoiceId, cancellationToken);
        return invoice?.ToDto();
    }
}
