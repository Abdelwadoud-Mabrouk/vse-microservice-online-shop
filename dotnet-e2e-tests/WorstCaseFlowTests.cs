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
            "SUCCESS",
            new { orderId, reason = "out_of_stock" },
            cts.Token);
        await client.WaitForAuditEventAsync(correlationId, "ORDER_CANCELLED", cts.Token);
    }

    [Fact]
    public async Task Invoice_service_ignores_invalid_payment_succeeded_event_without_creating_invoice()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(2));
        var settings = EndToEndTestSettings.FromEnvironment();
        using var client = new DotNetServiceClient(settings);
        using var kafka = new KafkaTestProducer(settings);

        var correlationId = Guid.NewGuid();
        var orderId = Guid.NewGuid();
        var customerEmail = $"invalid-payment-{DateTimeOffset.UtcNow:yyyyMMddHHmmssfff}@example.com";

        await client.AssertHealthyAsync(cts.Token);
        await kafka.PublishInvalidPaymentSucceededAsync(
            settings.BillingTopic,
            correlationId,
            orderId,
            customerEmail,
            cts.Token);

        await client.AssertNoInvoiceAsync(customerEmail, cts.Token);
    }

    [Fact]
    public async Task Parallel_identical_reservations_return_one_reservation_and_reduce_stock_once()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(2));
        var settings = EndToEndTestSettings.FromEnvironment();
        using var client = new DotNetServiceClient(settings);
        var orderId = Guid.NewGuid();

        await client.AssertHealthyAsync(cts.Token);
        await client.EnsureProductAsync(TestData.Chair, 10, cts.Token);

        var requests = Enumerable.Range(0, 8)
            .Select(_ => client.ReserveAsync(Guid.NewGuid(), orderId, TestData.Chair, 1, cts.Token));
        var reservationIds = await Task.WhenAll(requests);

        Assert.Single(reservationIds.Distinct());
        Assert.Equal(9, await client.GetAvailableQuantityAsync(TestData.Chair, cts.Token));
    }
}
