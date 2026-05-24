using System.Diagnostics;
using InvoiceService.Application;

namespace InvoiceService.Api.Endpoints;

public static class InvoiceEndpoints
{
    public static IEndpointRouteBuilder MapInvoiceEndpoints(this IEndpointRouteBuilder app)
    {
        app.MapPost("/events/payment-succeeded", CreateFromPaymentAsync);
        app.MapGet("/invoices/{invoiceId:guid}", GetInvoiceAsync);
        app.MapGet("/invoices", SearchInvoicesAsync);
        app.MapGet("/invoices/{invoiceId:guid}/pdf", GetInvoicePdfAsync);

        return app;
    }

    private static async Task<IResult> CreateFromPaymentAsync(
        PaymentSucceededRequest request,
        CreateInvoiceUseCase useCase,
        IAuditSink audit,
        IConfiguration configuration,
        HttpContext context,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        var correlationId = context.GetCorrelationId(request.CorrelationId);
        var logger = loggerFactory.CreateLogger("Invoice.PaymentEvents");
        var stopwatch = Stopwatch.StartNew();
        using var scope = logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "invoice-service",
            ["correlationId"] = correlationId,
            ["operation"] = "invoice.payment_succeeded.consume",
            ["route"] = "/events/payment-succeeded",
            ["eventType"] = "PaymentSucceeded",
            ["orderId"] = request.OrderId
        });

        logger.LogInformation(
            "Received payment succeeded event for order {OrderId}",
            request.OrderId);

        var validationError = request.Validate();
        if (validationError is not null)
        {
            logger.LogWarning(
                "Payment succeeded event rejected with status {Status}, errorCode {ErrorCode} in {DurationMs} ms",
                "VALIDATION_FAILED",
                "INVALID_PAYMENT_SUCCEEDED_EVENT",
                stopwatch.ElapsedMilliseconds);
            await audit.RecordAsync(correlationId, "INVOICE_REQUEST_REJECTED", "FAILURE", request, cancellationToken);
            return Results.ValidationProblem(validationError);
        }

        var retryCount = int.TryParse(configuration["INVOICE_RETRY_COUNT"], out var configuredRetryCount)
            ? configuredRetryCount
            : 3;

        var result = await useCase.ExecuteAsync(request.ToCommand(correlationId, retryCount), cancellationToken);
        if (!result.Success)
        {
            logger.LogWarning(
                "Invoice creation failed with status {Status}, errorCode {ErrorCode}, attempts {Attempts}, reason {Reason} in {DurationMs} ms",
                "BUSINESS_FAILED",
                "INVOICE_CREATION_FAILED",
                result.Attempts,
                result.Message,
                stopwatch.ElapsedMilliseconds);
            return Results.Problem(
                title: "Invoice generation failed",
                detail: result.Message,
                statusCode: StatusCodes.Status503ServiceUnavailable);
        }

        logger.LogInformation(
            "Invoice {InvoiceId} created with status {Status} in {DurationMs} ms",
            result.Invoice!.InvoiceId,
            "SUCCESS",
            stopwatch.ElapsedMilliseconds);
        return Results.Accepted($"/invoices/{result.Invoice!.InvoiceId}", result.Invoice);
    }

    private static async Task<IResult> GetInvoiceAsync(
        Guid invoiceId,
        GetInvoiceUseCase useCase,
        CancellationToken cancellationToken)
    {
        var invoice = await useCase.ExecuteAsync(invoiceId, cancellationToken);
        return invoice is null ? Results.NotFound() : Results.Ok(invoice);
    }

    private static async Task<IResult> SearchInvoicesAsync(
        string customerEmail,
        SearchInvoicesUseCase useCase,
        CancellationToken cancellationToken)
    {
        var invoices = await useCase.ExecuteAsync(customerEmail, cancellationToken);
        return Results.Ok(invoices);
    }

    private static async Task<IResult> GetInvoicePdfAsync(
        Guid invoiceId,
        IInvoiceRepository invoices,
        CancellationToken cancellationToken)
    {
        var invoice = await invoices.FindAsync(invoiceId, cancellationToken);
        if (invoice is null || string.IsNullOrWhiteSpace(invoice.DocumentPath) || !File.Exists(invoice.DocumentPath))
        {
            return Results.NotFound();
        }

        return Results.File(invoice.DocumentPath, "application/pdf", $"invoice-{invoice.InvoiceId}.pdf");
    }
}
