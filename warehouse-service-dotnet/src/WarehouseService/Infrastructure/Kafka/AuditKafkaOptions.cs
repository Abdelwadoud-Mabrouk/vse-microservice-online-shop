namespace WarehouseService.Infrastructure.Kafka;

public sealed class AuditKafkaOptions
{
    public string? BootstrapServers { get; init; }

    public string Topic { get; init; } = "audit-events";

    public bool Enabled => !string.IsNullOrWhiteSpace(BootstrapServers);
}
