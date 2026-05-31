public sealed class DotNetServiceClient : IDisposable
{
    private readonly HttpClient _audit;
    private readonly HttpClient _warehouse;
    private readonly HttpClient _invoice;
    private readonly JsonSerializerOptions _jsonOptions = new(JsonSerializerDefaults.Web);

    public DotNetServiceClient(EndToEndTestSettings settings)
    {
        _audit = new HttpClient { BaseAddress = settings.AuditUrl };
        _warehouse = new HttpClient { BaseAddress = settings.WarehouseUrl };
        _invoice = new HttpClient { BaseAddress = settings.InvoiceUrl };
    }

    public async Task AssertHealthyAsync(CancellationToken cancellationToken)
    {
        await AssertHealthyAsync(_audit, "audit-service", cancellationToken);
        await AssertHealthyAsync(_warehouse, "warehouse-service", cancellationToken);
        await AssertHealthyAsync(_invoice, "invoice-service", cancellationToken);
    }

    public async Task EnsureProductAsync(Product product, int availableQuantity, CancellationToken cancellationToken)
    {
        var request = new
        {
            productId = product.ProductId,
            sku = product.Sku,
            name = product.Name,
            category = product.Category,
            availableQuantity
        };

        var existing = await _warehouse.GetAsync($"/inventory/{product.ProductId}", cancellationToken);
        if (existing.StatusCode == HttpStatusCode.NotFound)
        {
            using var create = NewJsonRequest(HttpMethod.Post, "/inventory", request, Guid.NewGuid());
            var created = await _warehouse.SendAsync(create, cancellationToken);
            Assert.Equal(HttpStatusCode.Created, created.StatusCode);
            return;
        }

        Assert.Equal(HttpStatusCode.OK, existing.StatusCode);

        using var update = NewJsonRequest(HttpMethod.Put, $"/inventory/{product.ProductId}", request, Guid.NewGuid());
        var updated = await _warehouse.SendAsync(update, cancellationToken);
        Assert.Equal(HttpStatusCode.OK, updated.StatusCode);
    }

    public async Task<Guid> ReserveAsync(
        Guid correlationId,
        Guid orderId,
        Product product,
        int quantity,
        CancellationToken cancellationToken)
    {
        var request = new
        {
            orderId,
            items = new[]
            {
                new
                {
                    productId = product.ProductId,
                    sku = product.Sku,
                    quantity
                }
            }
        };

        using var message = NewJsonRequest(HttpMethod.Post, "/reservations", request, correlationId);
        var response = await _warehouse.SendAsync(message, cancellationToken);
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);

        using var document = await ReadJsonAsync(response, cancellationToken);
        return document.RootElement.GetProperty("reservationId").GetGuid();
    }

    public async Task<HttpStatusCode> TryReserveAsync(
        Guid correlationId,
        Guid orderId,
        Product product,
        int quantity,
        CancellationToken cancellationToken)
    {
        var request = new
        {
            orderId,
            items = new[]
            {
                new
                {
                    productId = product.ProductId,
                    sku = product.Sku,
                    quantity
                }
            }
        };

        using var message = NewJsonRequest(HttpMethod.Post, "/reservations", request, correlationId);
        var response = await _warehouse.SendAsync(message, cancellationToken);
        return response.StatusCode;
    }

    public async Task CommitReservationAsync(Guid correlationId, Guid reservationId, CancellationToken cancellationToken)
    {
        using var message = new HttpRequestMessage(HttpMethod.Post, $"/reservations/{reservationId}/commit");
        message.Headers.Add("X-Correlation-Id", correlationId.ToString());

        var response = await _warehouse.SendAsync(message, cancellationToken);
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    public async Task<Guid> WaitForInvoiceAsync(string customerEmail, CancellationToken cancellationToken)
    {
        var encodedEmail = Uri.EscapeDataString(customerEmail);
        return await WaitUntilAsync<Guid>(async () =>
        {
            var response = await _invoice.GetAsync($"/invoices?customerEmail={encodedEmail}", cancellationToken);
            response.EnsureSuccessStatusCode();

            using var document = await ReadJsonAsync(response, cancellationToken);
            if (document.RootElement.ValueKind != JsonValueKind.Array || document.RootElement.GetArrayLength() == 0)
            {
                return null;
            }

            return document.RootElement[0].GetProperty("invoiceId").GetGuid();
        }, $"Invoice for {customerEmail}", cancellationToken);
    }

    public async Task AssertNoInvoiceAsync(string customerEmail, CancellationToken cancellationToken)
    {
        await Task.Delay(TimeSpan.FromSeconds(4), cancellationToken);
        var encodedEmail = Uri.EscapeDataString(customerEmail);
        var response = await _invoice.GetAsync($"/invoices?customerEmail={encodedEmail}", cancellationToken);
        response.EnsureSuccessStatusCode();

        using var document = await ReadJsonAsync(response, cancellationToken);
        Assert.Equal(JsonValueKind.Array, document.RootElement.ValueKind);
        Assert.Equal(0, document.RootElement.GetArrayLength());
    }

    public async Task AssertInvoicePdfExistsAsync(Guid invoiceId, CancellationToken cancellationToken)
    {
        var response = await _invoice.GetAsync($"/invoices/{invoiceId}/pdf", cancellationToken);
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("application/pdf", response.Content.Headers.ContentType?.MediaType);
    }

    public async Task WaitForAuditEventAsync(Guid correlationId, string eventType, CancellationToken cancellationToken)
    {
        await WaitUntilAsync<bool>(async () =>
        {
            var events = await GetAuditEventsAsync(correlationId, cancellationToken);
            return events.Contains(eventType) ? true : null;
        }, $"Audit event {eventType}", cancellationToken);
    }

    private async Task<IReadOnlySet<string>> GetAuditEventsAsync(Guid correlationId, CancellationToken cancellationToken)
    {
        var response = await _audit.GetAsync($"/audit/orders/{correlationId}", cancellationToken);
        response.EnsureSuccessStatusCode();

        using var document = await ReadJsonAsync(response, cancellationToken);
        var events = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var item in document.RootElement.EnumerateArray())
        {
            if (item.TryGetProperty("eventType", out var eventType) && eventType.ValueKind == JsonValueKind.String)
            {
                events.Add(eventType.GetString()!);
            }
        }

        return events;
    }

    private static async Task<T> WaitUntilAsync<T>(
        Func<Task<T?>> action,
        string label,
        CancellationToken cancellationToken) where T : struct
    {
        var deadline = DateTimeOffset.UtcNow.AddSeconds(35);
        while (DateTimeOffset.UtcNow < deadline)
        {
            var result = await action();
            if (result.HasValue)
            {
                return result.Value;
            }

            await Task.Delay(TimeSpan.FromSeconds(1), cancellationToken);
        }

        throw new TimeoutException($"{label} was not available in time.");
    }

    private HttpRequestMessage NewJsonRequest(HttpMethod method, string url, object body, Guid correlationId)
    {
        var message = new HttpRequestMessage(method, url)
        {
            Content = JsonContent.Create(body, options: _jsonOptions)
        };
        message.Headers.Add("X-Correlation-Id", correlationId.ToString());
        return message;
    }

    private static async Task AssertHealthyAsync(HttpClient client, string serviceName, CancellationToken cancellationToken)
    {
        var response = await client.GetAsync("/health", cancellationToken);
        response.EnsureSuccessStatusCode();

        using var document = await ReadJsonAsync(response, cancellationToken);
        Assert.Equal(serviceName, document.RootElement.GetProperty("service").GetString());
        Assert.Equal("healthy", document.RootElement.GetProperty("status").GetString());
    }

    private static async Task<JsonDocument> ReadJsonAsync(HttpResponseMessage response, CancellationToken cancellationToken)
    {
        var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
        return await JsonDocument.ParseAsync(stream, cancellationToken: cancellationToken);
    }

    public void Dispose()
    {
        _audit.Dispose();
        _warehouse.Dispose();
        _invoice.Dispose();
    }
}
