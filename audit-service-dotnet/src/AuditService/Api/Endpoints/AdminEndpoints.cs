using System.Net.Http.Json;
using System.Text.Json;
using AuditService.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Npgsql;

namespace AuditService.Api.Endpoints;

public static class AdminEndpoints
{
    public static IEndpointRouteBuilder MapAdminEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/admin", () => Results.Redirect("/admin/index.html"));

        endpoints.MapGet("/admin/service-health", async (
            AuditDbContext auditDbContext,
            IConfiguration configuration,
            IHttpClientFactory httpClientFactory,
            ILoggerFactory loggerFactory,
            CancellationToken cancellationToken) =>
        {
            var logger = loggerFactory.CreateLogger("AdminHealth");
            var client = httpClientFactory.CreateClient("admin-health");
            var results = new List<ServiceHealthResult>
            {
                await CheckAuditDatabaseAsync(auditDbContext, logger, cancellationToken),
                await CheckWarehouseDatabaseAsync(configuration, logger, cancellationToken)
            };

            foreach (var check in ServiceChecks)
            {
                results.Add(await CheckServiceAsync(client, check, cancellationToken));
            }

            return Results.Ok(results);
        });

        endpoints.MapGet("/admin/warehouse/inventory", async (
            IHttpClientFactory httpClientFactory,
            CancellationToken cancellationToken) =>
        {
            var client = httpClientFactory.CreateClient("warehouse-admin");
            using var response = await client.GetAsync("/inventory", cancellationToken);
            return await ForwardJsonAsync(response, cancellationToken);
        });

        endpoints.MapPost("/admin/warehouse/inventory", async (
            JsonElement request,
            IHttpClientFactory httpClientFactory,
            HttpContext context,
            CancellationToken cancellationToken) =>
        {
            var client = httpClientFactory.CreateClient("warehouse-admin");
            using var message = new HttpRequestMessage(HttpMethod.Post, "/inventory")
            {
                Content = JsonContent.Create(request)
            };
            AddCorrelationId(message, context);
            using var response = await client.SendAsync(message, cancellationToken);
            return await ForwardJsonAsync(response, cancellationToken);
        });

        endpoints.MapPut("/admin/warehouse/inventory/{productId:guid}", async (
            Guid productId,
            JsonElement request,
            IHttpClientFactory httpClientFactory,
            HttpContext context,
            CancellationToken cancellationToken) =>
        {
            var client = httpClientFactory.CreateClient("warehouse-admin");
            using var message = new HttpRequestMessage(HttpMethod.Put, $"/inventory/{productId}")
            {
                Content = JsonContent.Create(request)
            };
            AddCorrelationId(message, context);
            using var response = await client.SendAsync(message, cancellationToken);
            return await ForwardJsonAsync(response, cancellationToken);
        });

        endpoints.MapPatch("/admin/warehouse/inventory/{productId:guid}/stock", async (
            Guid productId,
            JsonElement request,
            IHttpClientFactory httpClientFactory,
            HttpContext context,
            CancellationToken cancellationToken) =>
        {
            var client = httpClientFactory.CreateClient("warehouse-admin");
            using var message = new HttpRequestMessage(HttpMethod.Patch, $"/inventory/{productId}/stock")
            {
                Content = JsonContent.Create(request)
            };
            AddCorrelationId(message, context);
            using var response = await client.SendAsync(message, cancellationToken);
            return await ForwardJsonAsync(response, cancellationToken);
        });

        endpoints.MapDelete("/admin/warehouse/inventory/{productId:guid}", async (
            Guid productId,
            IHttpClientFactory httpClientFactory,
            HttpContext context,
            CancellationToken cancellationToken) =>
        {
            var client = httpClientFactory.CreateClient("warehouse-admin");
            using var message = new HttpRequestMessage(HttpMethod.Delete, $"/inventory/{productId}");
            AddCorrelationId(message, context);
            using var response = await client.SendAsync(message, cancellationToken);
            return await ForwardJsonAsync(response, cancellationToken);
        });

        return endpoints;
    }

    private static readonly ServiceHealthCheck[] ServiceChecks =
    [
        new("audit-service", "http://localhost:8080/health"),
     new("shop-service", "http://shop-service:8087/health"),
     new("billing-service", "http://billing-service:8089/api/v1/billing/health"),
        new("warehouse-service", "http://warehouse-service:8080/health"),
        new("invoice-service", "http://invoice-service:8080/health")
    ];

    private static void AddCorrelationId(HttpRequestMessage message, HttpContext context)
    {
        var correlationId = context.Request.Headers.TryGetValue("X-Correlation-Id", out var value)
            ? value.ToString()
            : Guid.NewGuid().ToString();

        message.Headers.TryAddWithoutValidation("X-Correlation-Id", correlationId);
    }

    private static async Task<IResult> ForwardJsonAsync(HttpResponseMessage response, CancellationToken cancellationToken)
    {
        if (response.Content.Headers.ContentLength == 0)
        {
            return Results.StatusCode((int)response.StatusCode);
        }

        var contentType = response.Content.Headers.ContentType?.MediaType;
        var body = await response.Content.ReadAsStringAsync(cancellationToken);
        return Results.Content(body, contentType ?? "application/json", statusCode: (int)response.StatusCode);
    }

    private static async Task<ServiceHealthResult> CheckAuditDatabaseAsync(
        AuditDbContext dbContext,
        ILogger logger,
        CancellationToken cancellationToken)
    {
        try
        {
            var canConnect = await dbContext.Database.CanConnectAsync(cancellationToken);
            return new ServiceHealthResult("audit-db", canConnect ? "healthy" : "unreachable", null);
        }
        catch (Exception exception)
        {
            logger.LogWarning(exception, "Audit database health check failed");
            return new ServiceHealthResult("audit-db", "unreachable", null);
        }
    }

    private static async Task<ServiceHealthResult> CheckWarehouseDatabaseAsync(
        IConfiguration configuration,
        ILogger logger,
        CancellationToken cancellationToken)
    {
        var connectionString = configuration["WAREHOUSE_DB_CONNECTION"]
            ?? string.Empty;

        if (string.IsNullOrWhiteSpace(connectionString))
        {
            logger.LogWarning("Warehouse database health check skipped because WAREHOUSE_DB_CONNECTION is missing");
            return new ServiceHealthResult("warehouse-db", "unreachable", null);
        }

        try
        {
            await using var connection = new NpgsqlConnection(connectionString);
            await connection.OpenAsync(cancellationToken);
            return new ServiceHealthResult("warehouse-db", "healthy", null);
        }
        catch (Exception exception)
        {
            logger.LogWarning(exception, "Warehouse database health check failed");
            return new ServiceHealthResult("warehouse-db", "unreachable", null);
        }
    }

    private static async Task<ServiceHealthResult> CheckServiceAsync(
        HttpClient client,
        ServiceHealthCheck check,
        CancellationToken cancellationToken)
    {
        try
        {
            using var response = await client.GetAsync(check.Url, cancellationToken);
            return new ServiceHealthResult(
                check.Name,
                response.IsSuccessStatusCode ? "healthy" : "unhealthy",
                (int)response.StatusCode);
        }
        catch
        {
            return new ServiceHealthResult(check.Name, "unreachable", null);
        }
    }

    private sealed record ServiceHealthCheck(string Name, string Url);

    private sealed record ServiceHealthResult(string Service, string Status, int? StatusCode);
}
