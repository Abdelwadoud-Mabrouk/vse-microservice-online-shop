using AuditService.Application;

namespace AuditService.Api;

public sealed record CreateAuditSnapshotRequest(
    Guid? CorrelationId,
    string? EventType,
    string? Service,
    Dictionary<string, object?>? Payload,
    Guid? PreviousEventId,
    string? Actor,
    string? StatusCode)
{
    public Dictionary<string, string[]>? Validate()
    {
        var errors = new Dictionary<string, string[]>();

        if (CorrelationId is null || CorrelationId == Guid.Empty)
        {
            errors[nameof(CorrelationId)] = ["A non-empty correlationId is required."];
        }

        if (string.IsNullOrWhiteSpace(EventType))
        {
            errors[nameof(EventType)] = ["eventType is required."];
        }

        if (string.IsNullOrWhiteSpace(Service))
        {
            errors[nameof(Service)] = ["service is required."];
        }

        if (string.IsNullOrWhiteSpace(StatusCode))
        {
            errors[nameof(StatusCode)] = ["statusCode is required."];
        }

        return errors.Count == 0 ? null : errors;
    }

    public CreateAuditSnapshotCommand ToCommand()
    {
        return new CreateAuditSnapshotCommand(
            CorrelationId!.Value,
            EventType!.Trim(),
            Service!.Trim(),
            Payload ?? new Dictionary<string, object?>(),
            PreviousEventId,
            Actor?.Trim() ?? "system",
            StatusCode!.Trim());
    }
}
