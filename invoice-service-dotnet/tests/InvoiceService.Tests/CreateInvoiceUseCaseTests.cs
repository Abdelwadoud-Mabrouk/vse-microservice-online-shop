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

    [Fact]
    public async Task ExecuteAsync_WhenTransactionWasAlreadyProcessed_ReturnsExistingInvoiceWithoutSecondDocument()
    {
        var repository = new InMemoryInvoiceRepository();
        var writer = new FakeDocumentWriter();
        var audit = new RecordingAuditSink();
        var useCase = new CreateInvoiceUseCase(repository, writer, audit);
        var payment = NewPayment();

        var first = await useCase.ExecuteAsync(new CreateInvoiceCommand(payment, 3), CancellationToken.None);
        var second = await useCase.ExecuteAsync(new CreateInvoiceCommand(payment, 3), CancellationToken.None);

        Assert.Equal(first.Invoice!.InvoiceId, second.Invoice!.InvoiceId);
        Assert.Equal(1, writer.Attempts);
        Assert.Single(repository.Invoices);
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
            CustomerDetails.Create("Max Mustermann", "customer@example.com", "Musterstrasse 12", "58507", "Luedenscheid", "Deutschland"),
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

        public Task<Invoice?> FindByTransactionIdAsync(string transactionId, CancellationToken cancellationToken)
        {
            return Task.FromResult(Invoices.SingleOrDefault(invoice => invoice.TransactionId == transactionId));
        }

        public Task<IReadOnlyList<Invoice>> FindByCustomerEmailAsync(string customerEmail, CancellationToken cancellationToken)
        {
            return Task.FromResult<IReadOnlyList<Invoice>>(
                Invoices
                    .Where(invoice => string.Equals(invoice.CustomerEmail, customerEmail, StringComparison.OrdinalIgnoreCase))
                    .ToArray());
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
