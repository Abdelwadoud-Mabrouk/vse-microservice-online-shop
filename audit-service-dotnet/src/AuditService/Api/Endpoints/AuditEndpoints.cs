using System.Diagnostics;
using AuditService.Api;
using AuditService.Application;

namespace AuditService.Api.Endpoints;

public static class AuditEndpoints
{
    public static IEndpointRouteBuilder MapAuditEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapPost("/audit/events", async (
            CreateAuditSnapshotRequest request,
            AppendAuditSnapshotUseCase useCase,
            ILoggerFactory loggerFactory,
            CancellationToken cancellationToken) =>
        {
            var logger = loggerFactory.CreateLogger("AuditEvents");
            var stopwatch = Stopwatch.StartNew();
            var validationError = request.Validate();
            if (validationError is not null)
            {
                logger.LogWarning(
                    "Audit event rejected with status {Status}, errorCode {ErrorCode}, correlationId {CorrelationId}, eventType {EventType}, durationMs {DurationMs}",
                    "VALIDATION_FAILED",
                    "INVALID_AUDIT_EVENT",
                    request.CorrelationId,
                    request.EventType,
                    stopwatch.ElapsedMilliseconds);

                return Results.ValidationProblem(validationError);
            }

            using var scope = logger.BeginScope(new Dictionary<string, object?>
            {
                ["service"] = "audit-service",
                ["correlationId"] = request.CorrelationId,
                ["operation"] = "audit.snapshot.append",
                ["route"] = "/audit/events",
                ["eventType"] = request.EventType,
                ["status"] = request.StatusCode,
                ["sourceService"] = request.Service
            });

            logger.LogInformation(
                "Audit event {EventType} received from {SourceService}",
                request.EventType,
                request.Service);

            var snapshot = await useCase.ExecuteAsync(request.ToCommand(), cancellationToken);
            logger.LogInformation(
                "Audit snapshot {AuditSnapshotId} persisted with status {Status} in {DurationMs} ms",
                snapshot.Id,
                "SUCCESS",
                stopwatch.ElapsedMilliseconds);

            return Results.Created($"/audit/events/{snapshot.Id}", snapshot);
        });

        endpoints.MapGet("/audit/orders/{correlationId:guid}", async (
            Guid correlationId,
            GetAuditTimelineUseCase useCase,
            ILoggerFactory loggerFactory,
            CancellationToken cancellationToken) =>
        {
            var logger = loggerFactory.CreateLogger("AuditTimeline");
            var stopwatch = Stopwatch.StartNew();
            using var scope = logger.BeginScope(new Dictionary<string, object?>
            {
                ["service"] = "audit-service",
                ["correlationId"] = correlationId,
                ["operation"] = "audit.timeline.query",
                ["route"] = "/audit/orders/{correlationId}"
            });

            var snapshots = await useCase.ExecuteAsync(correlationId, cancellationToken);
            logger.LogInformation(
                "Audit timeline query returned {EventCount} event(s) with status {Status} in {DurationMs} ms",
                snapshots.Count,
                "SUCCESS",
                stopwatch.ElapsedMilliseconds);

            return Results.Ok(snapshots);
        });

        endpoints.MapGet("/audit/orders", async (
            ListAuditOrderSummariesUseCase useCase,
            ILoggerFactory loggerFactory,
            CancellationToken cancellationToken) =>
        {
            var logger = loggerFactory.CreateLogger("AuditOrders");
            var stopwatch = Stopwatch.StartNew();
            using var scope = logger.BeginScope(new Dictionary<string, object?>
            {
                ["service"] = "audit-service",
                ["operation"] = "audit.orders.list",
                ["route"] = "/audit/orders"
            });

            var summaries = await useCase.ExecuteAsync(cancellationToken);
            logger.LogInformation(
                "Audit order summary query returned {OrderCount} order(s) with status {Status} in {DurationMs} ms",
                summaries.Count,
                "SUCCESS",
                stopwatch.ElapsedMilliseconds);

            return Results.Ok(summaries);
        });

        return endpoints;
    }
}
