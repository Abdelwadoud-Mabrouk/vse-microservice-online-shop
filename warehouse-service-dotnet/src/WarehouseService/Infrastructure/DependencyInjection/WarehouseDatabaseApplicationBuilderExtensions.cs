using Microsoft.EntityFrameworkCore;
using WarehouseService.Infrastructure.Persistence;

namespace WarehouseService.Infrastructure.DependencyInjection;

public static class WarehouseDatabaseApplicationBuilderExtensions
{
    public static async Task<WebApplication> InitializeWarehouseDatabaseAsync(this WebApplication app)
    {
        var applyMigrations = app.Configuration.GetValue("WAREHOUSE_APPLY_MIGRATIONS", true);
        if (!applyMigrations)
        {
            return app;
        }

        await using var scope = app.Services.CreateAsyncScope();
        var logger = scope.ServiceProvider.GetRequiredService<ILogger<WarehouseDbContext>>();
        var dbContext = scope.ServiceProvider.GetRequiredService<WarehouseDbContext>();

        logger.LogInformation(
            "Applying warehouse database migrations for service {Service}",
            "warehouse-service");

        await dbContext.Database.MigrateAsync();

        logger.LogInformation(
            "Warehouse database migrations applied for service {Service}",
            "warehouse-service");

        return app;
    }
}
