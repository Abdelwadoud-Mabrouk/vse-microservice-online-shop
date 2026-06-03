using Microsoft.EntityFrameworkCore;
using WarehouseService.Application;
using WarehouseService.Infrastructure;
using WarehouseService.Infrastructure.Kafka;
using WarehouseService.Infrastructure.Persistence;
using WarehouseService.Infrastructure.Persistence.Repositories;

namespace WarehouseService.Infrastructure.DependencyInjection;

public static class PersistenceServiceCollectionExtensions
{
    public static IServiceCollection AddWarehousePersistence(this IServiceCollection services, IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("WarehouseDb")
            ?? configuration["WAREHOUSE_DB_CONNECTION"]
            ?? throw new InvalidOperationException("Warehouse database connection is missing. Set WAREHOUSE_DB_CONNECTION.");

        services.AddDbContext<WarehouseDbContext>(options => options.UseNpgsql(connectionString));
        services.AddScoped<IWarehouseRepository, EfWarehouseRepository>();
        services.AddSingleton(new AuditKafkaOptions
        {
            BootstrapServers = configuration["KAFKA_BOOTSTRAP_SERVERS"],
            Topic = configuration["KAFKA_WAREHOUSE_TOPIC"] ?? "warehouse"
        });
        services.AddSingleton<IAuditSink, KafkaAuditSink>();

        return services;
    }
}
