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
            invoice.Items,
            invoice.CreatedAt,
            invoice.DocumentPath);
    }
}
