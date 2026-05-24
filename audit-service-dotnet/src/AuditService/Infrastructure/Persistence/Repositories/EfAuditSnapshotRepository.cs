using System.Text.Json;
using System.Diagnostics;
using AuditService.Application;
using AuditService.Domain;
using AuditService.Infrastructure.Persistence.Entities;
using Microsoft.EntityFrameworkCore;

namespace AuditService.Infrastructure.Persistence.Repositories;

public sealed class EfAuditSnapshotRepository(
    AuditDbContext dbContext,
    ILogger<EfAuditSnapshotRepository> logger) : IAuditSnapshotRepository
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    public async Task AppendAsync(AuditSnapshot snapshot, CancellationToken cancellationToken)
    {
        var stopwatch = Stopwatch.StartNew();
        using var scope = logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "audit-service",
            ["correlationId"] = snapshot.CorrelationId,
            ["operation"] = "audit.snapshot.persist",
            ["eventType"] = snapshot.EventType,
            ["status"] = snapshot.StatusCode,
            ["entityId"] = snapshot.Id,
            ["sourceService"] = snapshot.Service
        });

        dbContext.AuditSnapshots.Add(ToEntity(snapshot));
        await dbContext.SaveChangesAsync(cancellationToken);

        logger.LogInformation(
            "Audit snapshot {AuditSnapshotId} persisted for event {EventType} with status {Status} in {DurationMs} ms",
            snapshot.Id,
            snapshot.EventType,
            "SUCCESS",
            stopwatch.ElapsedMilliseconds);
    }

    public async Task<IReadOnlyList<AuditSnapshot>> FindByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken)
    {
        var snapshots = await dbContext.AuditSnapshots
            .AsNoTracking()
            .Where(snapshot => snapshot.CorrelationId == correlationId)
            .OrderBy(snapshot => snapshot.TimestampUtc)
            .ThenBy(snapshot => snapshot.Id)
            .ToListAsync(cancellationToken);

        return snapshots.Select(ToDomain).ToArray();
    }

    public async Task<IReadOnlyList<OrderAuditSummaryDto>> ListOrderSummariesAsync(CancellationToken cancellationToken)
    {
        var snapshots = await dbContext.AuditSnapshots
            .AsNoTracking()
            .OrderByDescending(snapshot => snapshot.TimestampUtc)
            .ThenByDescending(snapshot => snapshot.Id)
            .ToListAsync(cancellationToken);

        var summaries = snapshots
            .GroupBy(snapshot => snapshot.CorrelationId)
            .Select(group =>
            {
                var lastSnapshot = group.First();
                return new OrderAuditSummaryDto(
                    group.Key,
                    lastSnapshot.EventType,
                    lastSnapshot.StatusCode,
                    lastSnapshot.Service,
                    lastSnapshot.TimestampUtc,
                    group.Count());
            })
            .ToList();

        return summaries;
    }

    private static AuditSnapshotEntity ToEntity(AuditSnapshot snapshot)
    {
        return new AuditSnapshotEntity
        {
            Id = snapshot.Id,
            CorrelationId = snapshot.CorrelationId,
            EventType = snapshot.EventType,
            Service = snapshot.Service,
            TimestampUtc = snapshot.Timestamp,
            PayloadJson = JsonSerializer.Serialize(snapshot.Payload, JsonOptions),
            PreviousEventId = snapshot.PreviousEventId,
            Actor = snapshot.Actor,
            StatusCode = snapshot.StatusCode
        };
    }

    private static AuditSnapshot ToDomain(AuditSnapshotEntity entity)
    {
        var payload = JsonSerializer.Deserialize<Dictionary<string, object?>>(entity.PayloadJson, JsonOptions)
            ?? new Dictionary<string, object?>();

        return new AuditSnapshot(
            entity.Id,
            entity.CorrelationId,
            entity.EventType,
            entity.Service,
            entity.TimestampUtc,
            payload,
            entity.PreviousEventId,
            entity.Actor,
            entity.StatusCode);
    }
}
