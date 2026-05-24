using System.Diagnostics;
using System.Net.Http.Json;
using WarehouseService.Application;

namespace WarehouseService.Infrastructure;

public sealed class HttpAuditSink(HttpClient httpClient, IConfiguration configuration, ILogger<HttpAuditSink> logger) : IAuditSink
{
    private readonly string _baseUrl = configuration["AUDIT_SERVICE_URL"] ?? "http://audit-service:8080";

    public async Task RecordAsync(Guid correlationId, string eventType, string statusCode, object payload, CancellationToken cancellationToken)
    {
        var stopwatch = Stopwatch.StartNew();
        using var scope = logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "warehouse-service",
            ["correlationId"] = correlationId,
            ["operation"] = "audit.sink.write",
            ["eventType"] = eventType,
            ["status"] = statusCode
        });

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

            using var response = await httpClient.PostAsJsonAsync($"{_baseUrl}/audit/events", request, cancellationToken);
            if (!response.IsSuccessStatusCode)
            {
                logger.LogWarning(
                    "Audit sink write failed with status {Status}, errorCode {ErrorCode}, httpStatus {HttpStatus}, durationMs {DurationMs}",
                    "INTEGRATION_FAILED",
                    "AUDIT_SERVICE_REJECTED_EVENT",
                    (int)response.StatusCode,
                    stopwatch.ElapsedMilliseconds);
                return;
            }

            logger.LogInformation(
                "Audit event {EventType} written with status {Status} in {DurationMs} ms",
                eventType,
                "SUCCESS",
                stopwatch.ElapsedMilliseconds);
        }
        catch (Exception exception)
        {
            logger.LogWarning(
                exception,
                "Audit sink write failed with status {Status}, errorCode {ErrorCode}, exceptionType {ExceptionType}, durationMs {DurationMs}",
                "INTEGRATION_FAILED",
                "AUDIT_SERVICE_UNAVAILABLE",
                exception.GetType().Name,
                stopwatch.ElapsedMilliseconds);
        }
    }
}
