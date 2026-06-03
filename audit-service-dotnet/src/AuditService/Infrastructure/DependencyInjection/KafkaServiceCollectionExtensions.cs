using AuditService.Infrastructure.Kafka;

namespace AuditService.Infrastructure.DependencyInjection;

public static class KafkaServiceCollectionExtensions
{
    public static IServiceCollection AddAuditKafkaConsumers(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddSingleton(new KafkaConsumerOptions
        {
            BootstrapServers = configuration["KAFKA_BOOTSTRAP_SERVERS"],
            Topics = ParseTopics(configuration["KAFKA_AUDIT_TOPICS"]),
            ConsumerGroupId = configuration["KAFKA_AUDIT_CONSUMER_GROUP"] ?? "audit-service"
        });
        services.AddHostedService<AuditEventKafkaConsumer>();

        return services;
    }

    private static IReadOnlyList<string> ParseTopics(string? configuredTopics)
    {
        if (string.IsNullOrWhiteSpace(configuredTopics))
        {
            return ["orders", "billing", "warehouse", "invoices"];
        }

        var topics = configuredTopics
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Where(topic => !string.IsNullOrWhiteSpace(topic))
            .Distinct(StringComparer.Ordinal)
            .ToArray();

        return topics.Length == 0
            ? ["orders", "billing", "warehouse", "invoices"]
            : topics;
    }
}
