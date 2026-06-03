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

        await MarkInitialMigrationAppliedForExistingSchemaAsync(dbContext, cancellationToken: default);
        await dbContext.Database.MigrateAsync();

        logger.LogInformation(
            "Warehouse database migrations applied for service {Service}",
            "warehouse-service");

        return app;
    }

    private static async Task MarkInitialMigrationAppliedForExistingSchemaAsync(
        WarehouseDbContext dbContext,
        CancellationToken cancellationToken)
    {
        var connection = dbContext.Database.GetDbConnection();
        if (connection.State != System.Data.ConnectionState.Open)
        {
            await connection.OpenAsync(cancellationToken);
        }

        await using var command = connection.CreateCommand();
        command.CommandText = """
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = 'warehouse_products'
                ) THEN
                    CREATE TABLE IF NOT EXISTS "__EFMigrationsHistory" (
                        "MigrationId" character varying(150) NOT NULL,
                        "ProductVersion" character varying(32) NOT NULL,
                        CONSTRAINT "PK___EFMigrationsHistory" PRIMARY KEY ("MigrationId")
                    );

                    INSERT INTO "__EFMigrationsHistory" ("MigrationId", "ProductVersion")
                    SELECT '20260519191000_InitialWarehouseSchema', '10.0.4'
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM "__EFMigrationsHistory"
                        WHERE "MigrationId" = '20260519191000_InitialWarehouseSchema'
                    );
                END IF;
            END $$;
            """;

        await command.ExecuteNonQueryAsync(cancellationToken);
    }
}
