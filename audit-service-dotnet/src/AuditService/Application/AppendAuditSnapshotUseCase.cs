using AuditService.Domain;
using Microsoft.Extensions.Logging.Abstractions;

namespace AuditService.Application;

public sealed class AppendAuditSnapshotUseCase(
    IAuditSnapshotRepository snapshots,
    ILogger<AppendAuditSnapshotUseCase>? logger = null)
{
    private readonly ILogger<AppendAuditSnapshotUseCase> _logger = logger ?? NullLogger<AppendAuditSnapshotUseCase>.Instance;

    public async Task<AuditSnapshotDto> ExecuteAsync(CreateAuditSnapshotCommand command, CancellationToken cancellationToken)
    {
        using var scope = _logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "audit-service",
            ["correlationId"] = command.CorrelationId,
            ["operation"] = "audit.snapshot.append",
            ["eventType"] = command.EventType,
            ["status"] = command.StatusCode,
            ["sourceService"] = command.Service
        });

        if (command.EventId is { } eventId && eventId != Guid.Empty)
        {
            var existing = await snapshots.FindByIdAsync(eventId, cancellationToken);
            if (existing is not null)
            {
                _logger.LogInformation(
                    "Duplicate audit event {EventId} ignored; existing snapshot returned",
                    eventId);
                return existing.ToDto();
            }
        }

        var previousEventId = command.PreviousEventId;
        if (previousEventId is null)
        {
            var previousSnapshot = await snapshots.FindLatestByCorrelationIdAsync(
                command.CorrelationId,
                cancellationToken);
            previousEventId = previousSnapshot?.Id;
        }

        var snapshot = AuditSnapshot.Create(
            command.CorrelationId,
            command.EventType,
            command.Service,
            command.Payload,
            previousEventId,
            command.Actor,
            command.StatusCode,
            command.EventId);

        await snapshots.AppendAsync(snapshot, cancellationToken);
        _logger.LogInformation(
            "Audit snapshot {AuditSnapshotId} appended for event {EventType} from {SourceService} with status {Status}",
            snapshot.Id,
            snapshot.EventType,
            snapshot.Service,
            "SUCCESS");

        return snapshot.ToDto();
    }
}
