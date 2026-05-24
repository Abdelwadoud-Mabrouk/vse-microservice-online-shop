namespace AuditService.Application;

public sealed record OrderAuditSummaryDto(
    Guid CorrelationId,
    string LatestEventType,
    string LatestStatusCode,
    string LatestService,
    DateTimeOffset LastUpdatedAt,
    int EventCount);
