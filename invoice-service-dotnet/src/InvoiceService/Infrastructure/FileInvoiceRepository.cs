using System.Text.Json;
using InvoiceService.Application;
using InvoiceService.Domain;

namespace InvoiceService.Infrastructure;

public sealed class FileInvoiceRepository : IInvoiceRepository
{
    private readonly string _metadataDirectory;
    private readonly JsonSerializerOptions _jsonOptions = new(JsonSerializerDefaults.Web)
    {
        WriteIndented = true
    };

    public FileInvoiceRepository(IConfiguration configuration, IWebHostEnvironment environment)
    {
        var invoiceDirectory = configuration["INVOICE_DATA_DIR"]
            ?? Path.Combine(environment.ContentRootPath, "data", "invoices");

        _metadataDirectory = Path.Combine(invoiceDirectory, "metadata");
        Directory.CreateDirectory(_metadataDirectory);
    }

    public async Task SaveAsync(Invoice invoice, CancellationToken cancellationToken)
    {
        var path = GetPath(invoice.InvoiceId);
        var json = JsonSerializer.Serialize(invoice, _jsonOptions);
        await File.WriteAllTextAsync(path, json, cancellationToken);
    }

    public async Task<Invoice?> FindAsync(Guid invoiceId, CancellationToken cancellationToken)
    {
        var path = GetPath(invoiceId);
        if (!File.Exists(path))
        {
            return null;
        }

        await using var stream = File.OpenRead(path);
        return await JsonSerializer.DeserializeAsync<Invoice>(stream, _jsonOptions, cancellationToken);
    }

    public async Task<Invoice?> FindByTransactionIdAsync(string transactionId, CancellationToken cancellationToken)
    {
        foreach (var path in Directory.EnumerateFiles(_metadataDirectory, "*.json"))
        {
            await using var stream = File.OpenRead(path);
            var invoice = await JsonSerializer.DeserializeAsync<Invoice>(stream, _jsonOptions, cancellationToken);
            if (invoice is not null && string.Equals(invoice.TransactionId, transactionId, StringComparison.Ordinal))
            {
                return invoice;
            }
        }

        return null;
    }

    public async Task<IReadOnlyList<Invoice>> FindByCustomerEmailAsync(string customerEmail, CancellationToken cancellationToken)
    {
        var invoices = new List<Invoice>();
        var normalizedEmail = customerEmail.Trim().ToLowerInvariant();

        foreach (var path in Directory.EnumerateFiles(_metadataDirectory, "*.json"))
        {
            await using var stream = File.OpenRead(path);
            var invoice = await JsonSerializer.DeserializeAsync<Invoice>(stream, _jsonOptions, cancellationToken);
            if (invoice is not null && string.Equals(invoice.CustomerEmail, normalizedEmail, StringComparison.OrdinalIgnoreCase))
            {
                invoices.Add(invoice);
            }
        }

        return invoices;
    }

    private string GetPath(Guid invoiceId)
    {
        return Path.Combine(_metadataDirectory, $"{invoiceId}.json");
    }
}
