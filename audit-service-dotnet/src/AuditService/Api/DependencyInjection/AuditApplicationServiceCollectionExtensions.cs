using System.Text.Json;
using System.Text.Json.Serialization;
using AuditService.Application;

namespace AuditService.Api.DependencyInjection;

public static class AuditApplicationServiceCollectionExtensions
{
    public static IServiceCollection AddAuditApplicationServices(this IServiceCollection services)
    {
        services.ConfigureHttpJsonOptions(options =>
        {
            options.SerializerOptions.Converters.Add(new JsonStringEnumConverter());
            options.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        });

        services.AddScoped<AppendAuditSnapshotUseCase>();
        services.AddScoped<GetAuditTimelineUseCase>();
        services.AddScoped<ListAuditOrderSummariesUseCase>();

        services.AddProblemDetails();
        services.AddEndpointsApiExplorer();
        services.AddSwaggerGen();
        services.AddHttpClient("admin-health", client => client.Timeout = TimeSpan.FromSeconds(2));
        services.AddHttpClient("warehouse-admin", (provider, client) =>
        {
            var configuration = provider.GetRequiredService<IConfiguration>();
            var baseUrl = configuration["WAREHOUSE_SERVICE_URL"] ?? "http://warehouse-service:8080";
            client.BaseAddress = new Uri(baseUrl);
            client.Timeout = TimeSpan.FromSeconds(5);
        });

        return services;
    }
}
