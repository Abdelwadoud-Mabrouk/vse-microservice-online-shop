using AuditService.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace AuditService.Infrastructure.DependencyInjection;

public static class AuditDatabaseApplicationBuilderExtensions
{
    public static async Task<WebApplication> InitializeAuditDatabaseAsync(this WebApplication app)
    {
        var applyMigrations = app.Configuration.GetValue("AUDIT_APPLY_MIGRATIONS", true);
        if (!applyMigrations)
        {
            return app;
        }

        await using var scope = app.Services.CreateAsyncScope();
        var logger = scope.ServiceProvider.GetRequiredService<ILogger<AuditDbContext>>();
        var dbContext = scope.ServiceProvider.GetRequiredService<AuditDbContext>();

        logger.LogInformation(
            "Applying audit database migrations for service {Service}",
            "audit-service");

        await dbContext.Database.MigrateAsync();

        logger.LogInformation(
            "Audit database migrations applied for service {Service}",
            "audit-service");

        return app;
    }
}
