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

        var snapshot = AuditSnapshot.Create(
            command.CorrelationId,
            command.EventType,
            command.Service,
            command.Payload,
            command.PreviousEventId,
            command.Actor,
            command.StatusCode);

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
