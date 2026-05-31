using System.Diagnostics;
using System.Text.Json;
using Confluent.Kafka;
using WarehouseService.Application;

namespace WarehouseService.Infrastructure.Kafka;

public sealed class KafkaAuditSink : IAuditSink, IDisposable
{
    private readonly AuditKafkaOptions _options;
    private readonly ILogger<KafkaAuditSink> _logger;
    private readonly IProducer<string, string>? _producer;
    private readonly JsonSerializerOptions _jsonOptions = new(JsonSerializerDefaults.Web);

    public KafkaAuditSink(AuditKafkaOptions options, ILogger<KafkaAuditSink> logger)
    {
        _options = options;
        _logger = logger;

        if (_options.Enabled)
        {
            _producer = new ProducerBuilder<string, string>(new ProducerConfig
            {
                BootstrapServers = _options.BootstrapServers
            }).Build();
        }
    }

    public async Task RecordAsync(Guid correlationId, string eventType, string statusCode, object payload, CancellationToken cancellationToken)
    {
        var stopwatch = Stopwatch.StartNew();
        using var scope = _logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "warehouse-service",
            ["correlationId"] = correlationId,
            ["operation"] = "audit.kafka.publish",
            ["eventType"] = eventType,
            ["status"] = statusCode
        });

        if (_producer is null)
        {
            _logger.LogWarning(
                "Audit Kafka producer is disabled with status {Status}, errorCode {ErrorCode}",
                "INTEGRATION_DISABLED",
                "KAFKA_BOOTSTRAP_SERVERS_MISSING");
            return;
        }

        try
        {
            var envelope = new
            {
                eventId = Guid.NewGuid(),
                correlationId,
                eventType,
                service = "warehouse-service",
                timestamp = DateTimeOffset.UtcNow,
                payload,
                previousEventId = (Guid?)null,
                actor = "warehouse-service",
                statusCode
            };

            var message = JsonSerializer.Serialize(envelope, _jsonOptions);
            await _producer.ProduceAsync(
                _options.Topic,
                new Message<string, string>
                {
                    Key = correlationId.ToString(),
                    Value = message
                },
                cancellationToken);

            _logger.LogInformation(
                "Audit event {EventType} published to Kafka topic {Topic} with status {Status} in {DurationMs} ms",
                eventType,
                _options.Topic,
                "SUCCESS",
                stopwatch.ElapsedMilliseconds);
        }
        catch (Exception exception)
        {
            _logger.LogWarning(
                exception,
                "Audit Kafka publish failed with status {Status}, errorCode {ErrorCode}, exceptionType {ExceptionType}, durationMs {DurationMs}",
                "INTEGRATION_FAILED",
                "AUDIT_KAFKA_PUBLISH_FAILED",
                exception.GetType().Name,
                stopwatch.ElapsedMilliseconds);
        }
    }

    public void Dispose()
    {
        _producer?.Flush(TimeSpan.FromSeconds(5));
        _producer?.Dispose();
    }
}
