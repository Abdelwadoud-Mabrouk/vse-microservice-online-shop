using InvoiceService.Application;
using InvoiceService.Infrastructure.Kafka;

namespace InvoiceService.Infrastructure.DependencyInjection;

public static class InvoiceInfrastructureServiceCollectionExtensions
{
    public static IServiceCollection AddInvoiceInfrastructure(this IServiceCollection services)
    {
        services.AddSingleton<IInvoiceRepository, FileInvoiceRepository>();
        services.AddSingleton<IInvoiceDocumentWriter, PdfInvoiceDocumentWriter>();
        services.AddSingleton(provider =>
        {
            var configuration = provider.GetRequiredService<IConfiguration>();
            return new AuditKafkaOptions
            {
                BootstrapServers = configuration["KAFKA_BOOTSTRAP_SERVERS"],
                Topic = configuration["KAFKA_AUDIT_EVENTS_TOPIC"] ?? "audit-events"
            };
        });
        services.AddSingleton<IAuditSink, KafkaAuditSink>();
        services.AddSingleton(provider =>
        {
            var configuration = provider.GetRequiredService<IConfiguration>();
            return new KafkaConsumerOptions
            {
                BootstrapServers = configuration["KAFKA_BOOTSTRAP_SERVERS"],
                PaymentSucceededTopic = configuration["KAFKA_PAYMENT_SUCCEEDED_TOPIC"] ?? "payment-succeeded",
                ConsumerGroupId = configuration["KAFKA_INVOICE_CONSUMER_GROUP"] ?? "invoice-service"
            };
        });
        services.AddHostedService<PaymentSucceededKafkaConsumer>();

        return services;
    }
}
