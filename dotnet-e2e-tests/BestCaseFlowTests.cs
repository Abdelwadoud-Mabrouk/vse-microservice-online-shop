[Collection(E2ETestCollection.Name)]
public sealed class BestCaseFlowTests
{
    [Fact]
    public async Task Three_successful_orders_flow_through_rest_kafka_invoice_and_audit()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(3));
        var settings = EndToEndTestSettings.FromEnvironment();
        using var client = new DotNetServiceClient(settings);
        using var kafka = new KafkaTestProducer(settings);

        await client.AssertHealthyAsync(cts.Token);
        await client.EnsureProductAsync(TestData.Chair, 20, cts.Token);
        await client.EnsureProductAsync(TestData.Desk, 20, cts.Token);
        await client.EnsureProductAsync(TestData.Sofa, 20, cts.Token);

        await RunBestCaseAsync(client, kafka, settings, TestData.Chair, "best-chair", cts.Token);
        await RunBestCaseAsync(client, kafka, settings, TestData.Desk, "best-desk", cts.Token);
        await RunBestCaseAsync(client, kafka, settings, TestData.Sofa, "best-sofa", cts.Token);
    }

    private static async Task RunBestCaseAsync(
        DotNetServiceClient client,
        KafkaTestProducer kafka,
        EndToEndTestSettings settings,
        Product product,
        string label,
        CancellationToken cancellationToken)
    {
        var correlationId = Guid.NewGuid();
        var orderId = Guid.NewGuid();
        var customerEmail = $"{label}-{DateTimeOffset.UtcNow:yyyyMMddHHmmssfff}@example.com";
        var transactionId = $"tx-{label}-{Guid.NewGuid():N}";

        await kafka.PublishAuditEventAsync(
            settings.AuditEventsTopic,
            correlationId,
            "ORDER_CREATED",
            "shop-service",
            "SUCCESS",
            new { orderId },
            cancellationToken);
        await client.WaitForAuditEventAsync(correlationId, "ORDER_CREATED", cancellationToken);

        var reservationId = await client.ReserveAsync(correlationId, orderId, product, 1, cancellationToken);
        await client.WaitForAuditEventAsync(correlationId, "WAREHOUSE_RESERVED", cancellationToken);

        await kafka.PublishPaymentSucceededAsync(
            settings.PaymentSucceededTopic,
            correlationId,
            orderId,
            product,
            transactionId,
            customerEmail,
            cancellationToken);

        var invoiceId = await client.WaitForInvoiceAsync(customerEmail, cancellationToken);
        await client.AssertInvoicePdfExistsAsync(invoiceId, cancellationToken);
        await client.WaitForAuditEventAsync(correlationId, "INVOICE_CREATED", cancellationToken);

        await client.CommitReservationAsync(correlationId, reservationId, cancellationToken);
        await client.WaitForAuditEventAsync(correlationId, "WAREHOUSE_COMMITTED", cancellationToken);

        await kafka.PublishAuditEventAsync(
            settings.AuditEventsTopic,
            correlationId,
            "ORDER_COMPLETED",
            "shop-service",
            "SUCCESS",
            new { orderId, invoiceId },
            cancellationToken);
        await client.WaitForAuditEventAsync(correlationId, "ORDER_COMPLETED", cancellationToken);
    }
}
