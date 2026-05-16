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
    IReadOnlyList<InvoiceLineItem> Items,
    DateTimeOffset CreatedAt,
    string DocumentPath);

public sealed record InvoiceCreationResult(bool Success, int Attempts, string Message, InvoiceDto? Invoice);
