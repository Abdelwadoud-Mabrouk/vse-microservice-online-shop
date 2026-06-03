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
                Topic = configuration["KAFKA_INVOICES_TOPIC"] ?? "invoices"
            };
        });
        services.AddSingleton<IAuditSink, KafkaAuditSink>();
        services.AddSingleton(provider =>
        {
            var configuration = provider.GetRequiredService<IConfiguration>();
            return new KafkaConsumerOptions
            {
                BootstrapServers = configuration["KAFKA_BOOTSTRAP_SERVERS"],
                OrdersTopic = configuration["KAFKA_ORDERS_TOPIC"] ?? "orders",
                ConsumerGroupId = configuration["KAFKA_INVOICE_CONSUMER_GROUP"] ?? "invoice-service"
            };
        });
        services.AddHostedService<OrderCompletedKafkaConsumer>();

        return services;
    }
}
