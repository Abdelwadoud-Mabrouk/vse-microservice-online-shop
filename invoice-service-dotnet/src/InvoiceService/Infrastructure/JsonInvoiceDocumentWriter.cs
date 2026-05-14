using System.Text.Json;
using InvoiceService.Application;
using InvoiceService.Domain;

namespace InvoiceService.Infrastructure;

public sealed class JsonInvoiceDocumentWriter : IInvoiceDocumentWriter
{
    private readonly string _invoiceDirectory;
    private readonly bool _forceFailure;

    public JsonInvoiceDocumentWriter(IConfiguration configuration, IWebHostEnvironment environment)
    {
        _invoiceDirectory = configuration["INVOICE_DATA_DIR"]
            ?? Path.Combine(environment.ContentRootPath, "data", "invoices");
        Directory.CreateDirectory(_invoiceDirectory);

        _forceFailure = bool.TryParse(configuration["INVOICE_FORCE_FAILURE"], out var forceFailure) && forceFailure;
    }

    public async Task<string> WriteAsync(Invoice invoice, CancellationToken cancellationToken)
    {
        if (_forceFailure)
        {
            throw new InvalidOperationException("Invoice generation is forced to fail by configuration.");
        }

        var path = Path.Combine(_invoiceDirectory, $"{invoice.InvoiceId}.json");
        var json = JsonSerializer.Serialize(invoice, new JsonSerializerOptions(JsonSerializerDefaults.Web)
        {
            WriteIndented = true
        });

        await File.WriteAllTextAsync(path, json, cancellationToken);
        return path;
    }
}
