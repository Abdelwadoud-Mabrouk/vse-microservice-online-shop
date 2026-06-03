[Collection(E2ETestCollection.Name)]
public sealed class WorstCaseFlowTests
{
    [Fact]
    public async Task Warehouse_rejects_order_when_stock_is_not_available()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(2));
        var settings = EndToEndTestSettings.FromEnvironment();
        using var client = new DotNetServiceClient(settings);
        using var kafka = new KafkaTestProducer(settings);

        var correlationId = Guid.NewGuid();
        var orderId = Guid.NewGuid();

        await client.AssertHealthyAsync(cts.Token);
        await client.EnsureProductAsync(TestData.SoldOut, 0, cts.Token);

        await kafka.PublishServiceEventAsync(
            settings.OrdersTopic,
            correlationId,
            "ORDER_CREATED",
            "shop-service",
            "SUCCESS",
            new { orderId },
            cts.Token);
        await client.WaitForAuditEventAsync(correlationId, "ORDER_CREATED", cts.Token);

        var status = await client.TryReserveAsync(correlationId, orderId, TestData.SoldOut, 1, cts.Token);
        Assert.Equal(HttpStatusCode.Conflict, status);

        await client.WaitForAuditEventAsync(correlationId, "WAREHOUSE_OUT_OF_STOCK", cts.Token);

        await kafka.PublishServiceEventAsync(
            settings.OrdersTopic,
            correlationId,
            "ORDER_CANCELLED",
            "shop-service",
            "FAILURE",
            new { orderId, reason = "out_of_stock" },
            cts.Token);
        await client.WaitForAuditEventAsync(correlationId, "ORDER_CANCELLED", cts.Token);
    }

    [Fact]
    public async Task Invoice_service_ignores_invalid_order_completed_event_without_creating_invoice()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(2));
        var settings = EndToEndTestSettings.FromEnvironment();
        using var client = new DotNetServiceClient(settings);
        using var kafka = new KafkaTestProducer(settings);

        var correlationId = Guid.NewGuid();
        var orderId = Guid.NewGuid();
        var customerEmail = $"invalid-payment-{DateTimeOffset.UtcNow:yyyyMMddHHmmssfff}@example.com";

        await client.AssertHealthyAsync(cts.Token);
        await kafka.PublishInvalidOrderCompletedAsync(
            settings.OrdersTopic,
            correlationId,
            orderId,
            customerEmail,
            cts.Token);

        await client.AssertNoInvoiceAsync(customerEmail, cts.Token);
    }
}
