using System.Text.Json;
using System.Text.Json.Serialization;
using InvoiceService.Api;
using InvoiceService.Application;
using InvoiceService.Infrastructure;

var builder = WebApplication.CreateBuilder(args);

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new JsonStringEnumConverter());
    options.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
});

builder.Services.AddSingleton<IInvoiceRepository, InMemoryInvoiceRepository>();
builder.Services.AddSingleton<IInvoiceDocumentWriter, JsonInvoiceDocumentWriter>();
builder.Services.AddScoped<CreateInvoiceUseCase>();
builder.Services.AddScoped<GetInvoiceUseCase>();
builder.Services.AddHttpClient<IAuditSink, HttpAuditSink>();

builder.Services.AddProblemDetails();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();
app.UseMiddleware<CorrelationIdMiddleware>();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapGet("/health", () => Results.Ok(new { service = "invoice-service", status = "healthy" }));
app.MapGet("/", () => Results.Redirect("/swagger"));

app.MapPost("/events/payment-succeeded", async (
    PaymentSucceededRequest request,
    CreateInvoiceUseCase useCase,
    IAuditSink audit,
    IConfiguration configuration,
    HttpContext context,
    CancellationToken cancellationToken) =>
{
    var correlationId = context.GetCorrelationId(request.CorrelationId);
    var validationError = request.Validate();
    if (validationError is not null)
    {
        await audit.RecordAsync(correlationId, "INVOICE_REQUEST_REJECTED", "FAILURE", request, cancellationToken);
        return Results.ValidationProblem(validationError);
    }

    var retryCount = int.TryParse(configuration["INVOICE_RETRY_COUNT"], out var configuredRetryCount)
        ? configuredRetryCount
        : 3;

    var result = await useCase.ExecuteAsync(request.ToCommand(correlationId, retryCount), cancellationToken);
    if (!result.Success)
    {
        return Results.Problem(
            title: "Invoice generation failed",
            detail: result.Message,
            statusCode: StatusCodes.Status503ServiceUnavailable);
    }

    return Results.Accepted($"/invoices/{result.Invoice!.InvoiceId}", result.Invoice);
});

app.MapGet("/invoices/{invoiceId:guid}", async (
    Guid invoiceId,
    GetInvoiceUseCase useCase,
    CancellationToken cancellationToken) =>
{
    var invoice = await useCase.ExecuteAsync(invoiceId, cancellationToken);
    return invoice is null ? Results.NotFound() : Results.Ok(invoice);
});

app.Run();
