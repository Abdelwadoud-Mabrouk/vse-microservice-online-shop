using Confluent.Kafka;

public sealed class KafkaTestProducer : IDisposable
{
    private readonly IProducer<string, string> _producer;
    private readonly JsonSerializerOptions _jsonOptions = new(JsonSerializerDefaults.Web);

    public KafkaTestProducer(EndToEndTestSettings settings)
    {
        _producer = new ProducerBuilder<string, string>(new ProducerConfig
        {
            BootstrapServers = settings.KafkaBootstrapServers
        }).Build();
    }

    public async Task PublishAuditEventAsync(
        string topic,
        Guid correlationId,
        string eventType,
        string service,
        string statusCode,
        object payload,
        CancellationToken cancellationToken)
    {
        var envelope = new
        {
            eventId = Guid.NewGuid(),
            correlationId,
            eventType,
            service,
            timestamp = DateTimeOffset.UtcNow,
            payload,
            previousEventId = (Guid?)null,
            actor = service,
            statusCode
        };

        await PublishAsync(topic, correlationId, envelope, cancellationToken);
    }

    public async Task PublishPaymentSucceededAsync(
        string topic,
        Guid correlationId,
        Guid orderId,
        Product product,
        string transactionId,
        string customerEmail,
        CancellationToken cancellationToken)
    {
        var envelope = new
        {
            eventId = Guid.NewGuid(),
            correlationId,
            eventType = "PaymentSucceeded",
            service = "billing-service",
            timestamp = DateTimeOffset.UtcNow,
            payload = new
            {
                orderId,
                transactionId,
                amount = product.UnitPrice,
                currency = "EUR",
                customerEmail,
                customer = new
                {
                    name = "Max Mustermann",
                    email = customerEmail,
                    street = "Musterstrasse 12",
                    postalCode = "58507",
                    city = "Luedenscheid",
                    country = "Deutschland"
                },
                items = new[]
                {
                    new
                    {
                        sku = product.Sku,
                        name = product.Name,
                        quantity = 1,
                        unitPrice = product.UnitPrice
                    }
                }
            },
            previousEventId = (Guid?)null,
            actor = "billing-service",
            statusCode = "SUCCESS"
        };

        await PublishAsync(topic, correlationId, envelope, cancellationToken);
    }

    public async Task PublishInvalidPaymentSucceededAsync(
        string topic,
        Guid correlationId,
        Guid orderId,
        string customerEmail,
        CancellationToken cancellationToken)
    {
        var envelope = new
        {
            eventId = Guid.NewGuid(),
            correlationId,
            eventType = "PaymentSucceeded",
            service = "billing-service",
            timestamp = DateTimeOffset.UtcNow,
            payload = new
            {
                orderId,
                transactionId = "tx-invalid-payment",
                amount = 99.99m,
                currency = "EUR",
                customerEmail,
                customer = new
                {
                    name = "Fehler Kunde",
                    email = customerEmail,
                    street = "Teststrasse 1",
                    postalCode = "58507",
                    city = "Luedenscheid",
                    country = "Deutschland"
                }
            },
            previousEventId = (Guid?)null,
            actor = "billing-service",
            statusCode = "SUCCESS"
        };

        await PublishAsync(topic, correlationId, envelope, cancellationToken);
    }

    private async Task PublishAsync(string topic, Guid key, object message, CancellationToken cancellationToken)
    {
        var json = JsonSerializer.Serialize(message, _jsonOptions);
        await _producer.ProduceAsync(
            topic,
            new Message<string, string>
            {
                Key = key.ToString(),
                Value = json
            },
            cancellationToken);
    }

    public void Dispose()
    {
        _producer.Flush(TimeSpan.FromSeconds(5));
        _producer.Dispose();
    }
}
