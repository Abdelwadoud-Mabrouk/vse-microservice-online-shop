using AuditService.Application;
using AuditService.Infrastructure.Persistence;
using AuditService.Infrastructure.Persistence.Repositories;
using Microsoft.EntityFrameworkCore;

namespace AuditService.Infrastructure.DependencyInjection;

public static class PersistenceServiceCollectionExtensions
{
    public static IServiceCollection AddAuditPersistence(this IServiceCollection services, IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("AuditDb")
            ?? configuration["AUDIT_DB_CONNECTION"]
            ?? throw new InvalidOperationException("Audit database connection is missing. Set AUDIT_DB_CONNECTION.");

        services.AddDbContext<AuditDbContext>(options => options.UseNpgsql(connectionString));
        services.AddScoped<IAuditSnapshotRepository, EfAuditSnapshotRepository>();

        return services;
    }
}
