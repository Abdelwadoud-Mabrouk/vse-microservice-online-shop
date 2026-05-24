using InvoiceService.Domain;

namespace InvoiceService.Application;

internal static class InvoiceMapping
{
    public static InvoiceDto ToDto(this Invoice invoice)
    {
        return new InvoiceDto(
            invoice.InvoiceId,
            invoice.CorrelationId,
            invoice.OrderId,
            invoice.TransactionId,
            invoice.Amount,
            invoice.Currency,
            invoice.CustomerEmail,
            invoice.Customer,
            invoice.Items,
            invoice.CreatedAt,
            invoice.DocumentPath);
    }

    public static InvoiceSearchResultDto ToSearchResultDto(this Invoice invoice)
    {
        return new InvoiceSearchResultDto(
            invoice.InvoiceId,
            invoice.OrderId,
            invoice.CustomerEmail,
            invoice.Amount,
            invoice.Currency,
            invoice.CreatedAt,
            $"/invoices/{invoice.InvoiceId}/pdf");
    }
}
