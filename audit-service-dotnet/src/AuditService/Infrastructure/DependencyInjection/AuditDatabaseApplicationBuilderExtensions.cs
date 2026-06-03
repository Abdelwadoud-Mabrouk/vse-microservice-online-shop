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

        await MarkInitialMigrationAppliedForExistingSchemaAsync(dbContext, cancellationToken: default);
        await dbContext.Database.MigrateAsync();

        logger.LogInformation(
            "Audit database migrations applied for service {Service}",
            "audit-service");

        return app;
    }

    private static async Task MarkInitialMigrationAppliedForExistingSchemaAsync(
        AuditDbContext dbContext,
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
                      AND table_name = 'audit_snapshots'
                ) THEN
                    CREATE TABLE IF NOT EXISTS "__EFMigrationsHistory" (
                        "MigrationId" character varying(150) NOT NULL,
                        "ProductVersion" character varying(32) NOT NULL,
                        CONSTRAINT "PK___EFMigrationsHistory" PRIMARY KEY ("MigrationId")
                    );

                    INSERT INTO "__EFMigrationsHistory" ("MigrationId", "ProductVersion")
                    SELECT '20260519190000_InitialAuditSchema', '10.0.4'
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM "__EFMigrationsHistory"
                        WHERE "MigrationId" = '20260519190000_InitialAuditSchema'
                    );
                END IF;
            END $$;
            """;

        await command.ExecuteNonQueryAsync(cancellationToken);
    }
}
