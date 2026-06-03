namespace AuditService.Infrastructure.Kafka;

public sealed class KafkaConsumerOptions
{
    public string? BootstrapServers { get; init; }

    public IReadOnlyList<string> Topics { get; init; } = ["orders", "billing", "warehouse", "invoices"];

    public string ConsumerGroupId { get; init; } = "audit-service";

    public bool Enabled => !string.IsNullOrWhiteSpace(BootstrapServers) && Topics.Count > 0;
}
