namespace AuditService.Infrastructure.Kafka;

public sealed class KafkaConsumerOptions
{
    public string? BootstrapServers { get; init; }

    public string AuditEventsTopic { get; init; } = "audit-events";

    public string ConsumerGroupId { get; init; } = "audit-service";

    public bool Enabled => !string.IsNullOrWhiteSpace(BootstrapServers);
}
