namespace WarehouseService.Application;

public interface IAuditSink
{
    Task RecordAsync(Guid correlationId, string eventType, string statusCode, object payload, CancellationToken cancellationToken);
}
