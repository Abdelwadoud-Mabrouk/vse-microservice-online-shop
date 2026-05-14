using System.Net.Http.Json;
using WarehouseService.Application;

namespace WarehouseService.Infrastructure;

public sealed class HttpAuditSink(HttpClient httpClient, IConfiguration configuration, ILogger<HttpAuditSink> logger) : IAuditSink
{
    private readonly string _baseUrl = configuration["AUDIT_SERVICE_URL"] ?? "http://audit-service:8080";

    public async Task RecordAsync(Guid correlationId, string eventType, string statusCode, object payload, CancellationToken cancellationToken)
    {
        try
        {
            var request = new
            {
                correlationId,
                eventType,
                service = "warehouse-service",
                payload,
                actor = "warehouse-service",
                statusCode
            };

            await httpClient.PostAsJsonAsync($"{_baseUrl}/audit/events", request, cancellationToken);
        }
        catch (Exception exception)
        {
            logger.LogWarning(exception, "Could not write audit snapshot for {EventType}", eventType);
        }
    }
}
