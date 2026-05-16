using InvoiceService.Application;
using InvoiceService.Domain;

namespace InvoiceService.Tests;

public sealed class CreateInvoiceUseCaseTests
{
    [Fact]
    public async Task ExecuteAsync_WhenDocumentCanBeWritten_SavesInvoiceAndWritesAuditEvents()
    {
        var repository = new InMemoryInvoiceRepository();
        var writer = new FakeDocumentWriter();
        var audit = new RecordingAuditSink();
        var useCase = new CreateInvoiceUseCase(repository, writer, audit);

        var result = await useCase.ExecuteAsync(new CreateInvoiceCommand(NewPayment(), RetryCount: 3), CancellationToken.None);

        Assert.True(result.Success);
        Assert.NotNull(result.Invoice);
        Assert.Equal(1, writer.Attempts);
        Assert.Contains(audit.Events, item => item.EventType == "INVOICE_CREATION_STARTED");
        Assert.Contains(audit.Events, item => item.EventType == "INVOICE_CREATED");
    }

    [Fact]
    public async Task ExecuteAsync_WhenDocumentWritingKeepsFailing_RetriesAndWritesFailureAuditEvent()
    {
        var repository = new InMemoryInvoiceRepository();
        var writer = new FakeDocumentWriter(shouldFail: true);
        var audit = new RecordingAuditSink();
        var useCase = new CreateInvoiceUseCase(repository, writer, audit);

        var result = await useCase.ExecuteAsync(new CreateInvoiceCommand(NewPayment(), RetryCount: 3), CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal(3, writer.Attempts);
        Assert.Contains(audit.Events, item => item.EventType == "INVOICE_CREATION_FAILED" && item.StatusCode == "FAILURE");
    }

    private static PaymentSucceeded NewPayment()
    {
        return PaymentSucceeded.Create(
            Guid.NewGuid(),
            Guid.NewGuid(),
            "tx-123",
            129.99m,
            "EUR",
            "customer@example.com",
            [InvoiceLineItem.Create("SKU-CHAIR", "Chair", 1, 129.99m)]);
    }

    private sealed class InMemoryInvoiceRepository : IInvoiceRepository
    {
        public List<Invoice> Invoices { get; } = [];

        public Task SaveAsync(Invoice invoice, CancellationToken cancellationToken)
        {
            Invoices.Add(invoice);
            return Task.CompletedTask;
        }

        public Task<Invoice?> FindAsync(Guid invoiceId, CancellationToken cancellationToken)
        {
            return Task.FromResult(Invoices.FirstOrDefault(invoice => invoice.InvoiceId == invoiceId));
        }
    }

    private sealed class FakeDocumentWriter(bool shouldFail = false) : IInvoiceDocumentWriter
    {
        public int Attempts { get; private set; }

        public Task<string> WriteAsync(Invoice invoice, CancellationToken cancellationToken)
        {
            Attempts++;
            if (shouldFail)
            {
                throw new InvalidOperationException("Could not write invoice.");
            }

            return Task.FromResult($"/tmp/{invoice.InvoiceId}.json");
        }
    }

    private sealed class RecordingAuditSink : IAuditSink
    {
        public List<(Guid CorrelationId, string EventType, string StatusCode)> Events { get; } = [];

        public Task RecordAsync(Guid correlationId, string eventType, string statusCode, object payload, CancellationToken cancellationToken)
        {
            Events.Add((correlationId, eventType, statusCode));
            return Task.CompletedTask;
        }
    }
}
