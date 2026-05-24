namespace InvoiceService.Application;

public sealed class SearchInvoicesUseCase(IInvoiceRepository invoices)
{
    public async Task<IReadOnlyList<InvoiceSearchResultDto>> ExecuteAsync(string customerEmail, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(customerEmail))
        {
            return [];
        }

        var result = await invoices.FindByCustomerEmailAsync(customerEmail, cancellationToken);
        return result
            .OrderByDescending(invoice => invoice.CreatedAt)
            .Select(invoice => invoice.ToSearchResultDto())
            .ToArray();
    }
}
