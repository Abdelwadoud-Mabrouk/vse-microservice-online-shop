namespace InvoiceService.Infrastructure.Kafka;

public sealed class KafkaConsumerOptions
{
    public string? BootstrapServers { get; init; }

    public string PaymentSucceededTopic { get; init; } = "payment-succeeded";

    public string ConsumerGroupId { get; init; } = "invoice-service";

    public bool Enabled => !string.IsNullOrWhiteSpace(BootstrapServers);
}
