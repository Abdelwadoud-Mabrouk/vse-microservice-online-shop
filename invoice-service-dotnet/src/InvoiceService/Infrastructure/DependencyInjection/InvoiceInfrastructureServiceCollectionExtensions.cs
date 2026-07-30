using InvoiceService.Application;
using InvoiceService.Infrastructure.Kafka;

namespace InvoiceService.Infrastructure.DependencyInjection;

public static class InvoiceInfrastructureServiceCollectionExtensions
{
    public static IServiceCollection AddInvoiceInfrastructure(this IServiceCollection services)
    {
        services.AddSingleton<IInvoiceRepository, FileInvoiceRepository>();
        services.AddSingleton<IInvoiceDocumentWriter, PdfInvoiceDocumentWriter>();
        services.AddHttpClient<IOrderDetailsProvider, ShopOrderDetailsClient>((provider, client) =>
        {
            var configuration = provider.GetRequiredService<IConfiguration>();
            client.BaseAddress = new Uri(configuration["SHOP_SERVICE_URL"] ?? "http://shop-service:8087/");
            client.Timeout = TimeSpan.FromSeconds(10);
        });
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
                PaymentSucceededTopic = configuration["KAFKA_PAYMENT_SUCCEEDED_TOPIC"]
                    ?? configuration["KAFKA_BILLING_TOPIC"]
                    ?? "billing",
                ConsumerGroupId = configuration["KAFKA_INVOICE_CONSUMER_GROUP"] ?? "invoice-service"
            };
        });
        services.AddHostedService<PaymentSucceededKafkaConsumer>();

        return services;
    }
}
