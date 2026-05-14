using System.Text.Json;
using Npgsql;
using AuditService.Application;
using AuditService.Domain;

namespace AuditService.Infrastructure;

public sealed class PostgresAuditSnapshotRepository : IAuditSnapshotRepository
{
    private readonly NpgsqlDataSource _dataSource;
    private readonly SemaphoreSlim _schemaLock = new(1, 1);
    private bool _schemaCreated;

    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    public PostgresAuditSnapshotRepository(IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("AuditDb")
            ?? configuration["AUDIT_DB_CONNECTION"]
            ?? "Host=localhost;Port=5432;Database=audit;Username=audit;Password=audit";

        _dataSource = NpgsqlDataSource.Create(connectionString);
    }

    public async Task AppendAsync(AuditSnapshot snapshot, CancellationToken cancellationToken)
    {
        await EnsureSchemaAsync(cancellationToken);

        const string sql = """
            insert into audit_snapshots (
                id, correlation_id, event_type, service, timestamp_utc,
                payload, previous_event_id, actor, status_code
            )
            values (
                @id, @correlation_id, @event_type, @service, @timestamp_utc,
                cast(@payload as jsonb), @previous_event_id, @actor, @status_code
            );
            """;

        await using var command = _dataSource.CreateCommand(sql);
        command.Parameters.AddWithValue("id", snapshot.Id);
        command.Parameters.AddWithValue("correlation_id", snapshot.CorrelationId);
        command.Parameters.AddWithValue("event_type", snapshot.EventType);
        command.Parameters.AddWithValue("service", snapshot.Service);
        command.Parameters.AddWithValue("timestamp_utc", snapshot.Timestamp.UtcDateTime);
        command.Parameters.AddWithValue("payload", JsonSerializer.Serialize(snapshot.Payload, JsonOptions));
        command.Parameters.AddWithValue("previous_event_id", snapshot.PreviousEventId.HasValue ? snapshot.PreviousEventId.Value : (object)DBNull.Value);
        command.Parameters.AddWithValue("actor", snapshot.Actor);
        command.Parameters.AddWithValue("status_code", snapshot.StatusCode);

        await command.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<AuditSnapshot>> FindByCorrelationIdAsync(Guid correlationId, CancellationToken cancellationToken)
    {
        await EnsureSchemaAsync(cancellationToken);

        const string sql = """
            select id, correlation_id, event_type, service, timestamp_utc,
                   payload, previous_event_id, actor, status_code
            from audit_snapshots
            where correlation_id = @correlation_id
            order by timestamp_utc asc, id asc;
            """;

        await using var command = _dataSource.CreateCommand(sql);
        command.Parameters.AddWithValue("correlation_id", correlationId);

        var snapshots = new List<AuditSnapshot>();
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            var payload = JsonSerializer.Deserialize<Dictionary<string, object?>>(reader.GetString(5), JsonOptions)
                ?? new Dictionary<string, object?>();

            snapshots.Add(new AuditSnapshot(
                reader.GetGuid(0),
                reader.GetGuid(1),
                reader.GetString(2),
                reader.GetString(3),
                new DateTimeOffset(DateTime.SpecifyKind(reader.GetDateTime(4), DateTimeKind.Utc)),
                payload,
                reader.IsDBNull(6) ? null : reader.GetGuid(6),
                reader.GetString(7),
                reader.GetString(8)));
        }

        return snapshots;
    }

    private async Task EnsureSchemaAsync(CancellationToken cancellationToken)
    {
        if (_schemaCreated)
        {
            return;
        }

        await _schemaLock.WaitAsync(cancellationToken);
        try
        {
            if (_schemaCreated)
            {
                return;
            }

            const string sql = """
                create table if not exists audit_snapshots (
                    id uuid primary key,
                    correlation_id uuid not null,
                    event_type varchar(120) not null,
                    service varchar(120) not null,
                    timestamp_utc timestamptz not null,
                    payload jsonb not null,
                    previous_event_id uuid null,
                    actor varchar(120) not null,
                    status_code varchar(40) not null
                );

                create index if not exists ix_audit_snapshots_correlation_timestamp
                    on audit_snapshots (correlation_id, timestamp_utc);
                """;

            await using var command = _dataSource.CreateCommand(sql);
            await command.ExecuteNonQueryAsync(cancellationToken);
            _schemaCreated = true;
        }
        finally
        {
            _schemaLock.Release();
        }
    }
}
