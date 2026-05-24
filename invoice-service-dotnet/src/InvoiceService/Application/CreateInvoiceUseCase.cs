using InvoiceService.Domain;
using Microsoft.Extensions.Logging.Abstractions;

namespace InvoiceService.Application;

public sealed class CreateInvoiceUseCase(
    IInvoiceRepository invoices,
    IInvoiceDocumentWriter documents,
    IAuditSink audit,
    ILogger<CreateInvoiceUseCase>? logger = null)
{
    private readonly ILogger<CreateInvoiceUseCase> _logger = logger ?? NullLogger<CreateInvoiceUseCase>.Instance;

    public async Task<InvoiceCreationResult> ExecuteAsync(CreateInvoiceCommand command, CancellationToken cancellationToken)
    {
        var payment = command.Payment;
        using var scope = _logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "invoice-service",
            ["correlationId"] = payment.CorrelationId,
            ["operation"] = "invoice.create",
            ["eventType"] = "PaymentSucceeded",
            ["orderId"] = payment.OrderId
        });

        _logger.LogInformation(
            "Invoice creation started for order {OrderId}",
            payment.OrderId);

        await audit.RecordAsync(payment.CorrelationId, "INVOICE_CREATION_STARTED", "SUCCESS", new
        {
            payment.OrderId,
            payment.TransactionId,
            payment.Amount,
            payment.Currency
        }, cancellationToken);

        var attempts = Math.Max(1, command.RetryCount);
        Exception? lastException = null;

        for (var attempt = 1; attempt <= attempts; attempt++)
        {
            try
            {
                var invoice = Invoice.FromPayment(payment);
                var documentPath = await documents.WriteAsync(invoice, cancellationToken);
                invoice = invoice with { DocumentPath = documentPath };

                await invoices.SaveAsync(invoice, cancellationToken);

                var dto = invoice.ToDto();
                await audit.RecordAsync(payment.CorrelationId, "INVOICE_CREATED", "SUCCESS", dto, cancellationToken);

                _logger.LogInformation(
                    "Invoice {InvoiceId} persisted for order {OrderId} with status {Status}",
                    invoice.InvoiceId,
                    payment.OrderId,
                    "SUCCESS");

                return new InvoiceCreationResult(true, attempt, "Invoice created.", dto);
            }
            catch (Exception exception)
            {
                lastException = exception;
                _logger.LogWarning(
                    exception,
                    "Invoice creation attempt {Attempt} of {MaxAttempts} failed with errorCode {ErrorCode} and exceptionType {ExceptionType}",
                    attempt,
                    attempts,
                    "INVOICE_DOCUMENT_WRITE_FAILED",
                    exception.GetType().Name);

                if (attempt < attempts)
                {
                    await Task.Delay(TimeSpan.FromMilliseconds(250 * attempt), cancellationToken);
                }
            }
        }

        var message = lastException?.Message ?? "Invoice creation failed.";
        _logger.LogError(
            lastException,
            "Invoice creation failed for order {OrderId} with status {Status}, errorCode {ErrorCode}, attempts {Attempts}, exceptionType {ExceptionType}",
            payment.OrderId,
            "FAILURE",
            "INVOICE_CREATION_FAILED",
            attempts,
            lastException?.GetType().Name);

        await audit.RecordAsync(payment.CorrelationId, "INVOICE_CREATION_FAILED", "FAILURE", new
        {
            payment.OrderId,
            Attempts = attempts,
            Message = message
        }, cancellationToken);

        return new InvoiceCreationResult(false, attempts, message, null);
    }
}
