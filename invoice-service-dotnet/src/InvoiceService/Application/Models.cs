using InvoiceService.Domain;

namespace InvoiceService.Application;

public sealed record CreateInvoiceCommand(PaymentSucceeded Payment, int RetryCount);

public sealed record InvoiceDto(
    Guid InvoiceId,
    Guid CorrelationId,
    Guid OrderId,
    string TransactionId,
    decimal Amount,
    string Currency,
    string CustomerEmail,
    CustomerDetails Customer,
    IReadOnlyList<InvoiceLineItem> Items,
    DateTimeOffset CreatedAt,
    string DocumentPath);

public sealed record InvoiceCreationResult(bool Success, int Attempts, string Message, InvoiceDto? Invoice);

public sealed record InvoiceSearchResultDto(
    Guid InvoiceId,
    Guid OrderId,
    string CustomerEmail,
    decimal Amount,
    string Currency,
    DateTimeOffset CreatedAt,
    string PdfUrl);
