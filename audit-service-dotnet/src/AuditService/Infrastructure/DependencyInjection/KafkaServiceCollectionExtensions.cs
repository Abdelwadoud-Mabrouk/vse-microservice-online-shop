using AuditService.Infrastructure.Kafka;

namespace AuditService.Infrastructure.DependencyInjection;

public static class KafkaServiceCollectionExtensions
{
    public static IServiceCollection AddAuditKafkaConsumers(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddSingleton(new KafkaConsumerOptions
        {
            BootstrapServers = configuration["KAFKA_BOOTSTRAP_SERVERS"],
            AuditEventsTopic = configuration["KAFKA_AUDIT_EVENTS_TOPIC"] ?? "audit-events",
            ConsumerGroupId = configuration["KAFKA_AUDIT_CONSUMER_GROUP"] ?? "audit-service"
        });
        services.AddHostedService<AuditEventKafkaConsumer>();

        return services;
    }
}
