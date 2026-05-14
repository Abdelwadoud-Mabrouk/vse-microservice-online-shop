using InvoiceService.Domain;

namespace InvoiceService.Application;

public sealed class CreateInvoiceUseCase(
    IInvoiceRepository invoices,
    IInvoiceDocumentWriter documents,
    IAuditSink audit)
{
    public async Task<InvoiceCreationResult> ExecuteAsync(CreateInvoiceCommand command, CancellationToken cancellationToken)
    {
        var payment = command.Payment;

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

                return new InvoiceCreationResult(true, attempt, "Invoice created.", dto);
            }
            catch (Exception exception)
            {
                lastException = exception;
                if (attempt < attempts)
                {
                    await Task.Delay(TimeSpan.FromMilliseconds(250 * attempt), cancellationToken);
                }
            }
        }

        var message = lastException?.Message ?? "Invoice creation failed.";
        await audit.RecordAsync(payment.CorrelationId, "INVOICE_CREATION_FAILED", "FAILURE", new
        {
            payment.OrderId,
            Attempts = attempts,
            Message = message
        }, cancellationToken);

        return new InvoiceCreationResult(false, attempts, message, null);
    }
}
