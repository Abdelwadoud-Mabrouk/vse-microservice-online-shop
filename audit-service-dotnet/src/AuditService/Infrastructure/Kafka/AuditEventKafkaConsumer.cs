using System.Diagnostics;
using Confluent.Kafka;
using AuditService.Application;

namespace AuditService.Infrastructure.Kafka;

public sealed class AuditEventKafkaConsumer(
    IServiceScopeFactory scopeFactory,
    KafkaConsumerOptions options,
    ILogger<AuditEventKafkaConsumer> logger) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!options.Enabled)
        {
            logger.LogInformation(
                "Kafka consumer disabled for operation {Operation}. Set KAFKA_BOOTSTRAP_SERVERS to enable it.",
                "audit.kafka.audit_events.consume");
            return;
        }

        await Task.Yield();

        var config = new ConsumerConfig
        {
            BootstrapServers = options.BootstrapServers,
            GroupId = options.ConsumerGroupId,
            EnableAutoCommit = false,
            AutoOffsetReset = AutoOffsetReset.Earliest
        };

        using var consumer = new ConsumerBuilder<string, string>(config)
            .SetLogHandler((_, _) => { })
            .SetErrorHandler((_, error) =>
            {
                if (error.IsFatal)
                {
                    logger.LogError(
                        "Kafka client reported fatal error with status {Status}, errorCode {ErrorCode}, reason {Reason}",
                        "INTEGRATION_FAILED",
                        error.Code.ToString(),
                        error.Reason);
                }
            })
            .Build();
        consumer.Subscribe(options.AuditEventsTopic);

        logger.LogInformation(
            "Kafka consumer subscribed to topic {Topic} with group {ConsumerGroupId}",
            options.AuditEventsTopic,
            options.ConsumerGroupId);

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                var result = consumer.Consume(stoppingToken);
                await ProcessMessageAsync(result, consumer, stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (ConsumeException exception)
            {
                var level = exception.Error.Code == ErrorCode.UnknownTopicOrPart
                    ? LogLevel.Debug
                    : LogLevel.Warning;

                logger.Log(
                    level,
                    exception,
                    "Kafka consume failed with status {Status}, errorCode {ErrorCode}, exceptionType {ExceptionType}",
                    "INTEGRATION_FAILED",
                    exception.Error.Code.ToString(),
                    exception.GetType().Name);
            }
            catch (Exception exception)
            {
                logger.LogError(
                    exception,
                    "Unexpected Kafka consumer failure with status {Status}, errorCode {ErrorCode}, exceptionType {ExceptionType}",
                    "FAILURE",
                    "KAFKA_CONSUMER_UNEXPECTED_ERROR",
                    exception.GetType().Name);
            }
        }

        consumer.Close();
    }

    private async Task ProcessMessageAsync(
        ConsumeResult<string, string> result,
        IConsumer<string, string> consumer,
        CancellationToken cancellationToken)
    {
        var stopwatch = Stopwatch.StartNew();
        var operation = "audit.kafka.audit_events.consume";

        logger.LogInformation(
            "Kafka message received from topic {Topic}, partition {Partition}, offset {Offset}",
            result.Topic,
            result.Partition.Value,
            result.Offset.Value);

        try
        {
            var auditEvent = AuditEventEnvelopeDeserializer.Deserialize(result.Message.Value);
            using var scope = logger.BeginScope(new Dictionary<string, object?>
            {
                ["service"] = "audit-service",
                ["correlationId"] = auditEvent.CorrelationId,
                ["operation"] = operation,
                ["eventType"] = auditEvent.EventType,
                ["status"] = auditEvent.StatusCode,
                ["sourceService"] = auditEvent.Service
            });

            logger.LogInformation(
                "Kafka audit event deserialized for event {EventType} from {SourceService}",
                auditEvent.EventType,
                auditEvent.Service);

            using var serviceScope = scopeFactory.CreateScope();
            var useCase = serviceScope.ServiceProvider.GetRequiredService<AppendAuditSnapshotUseCase>();
            var snapshot = await useCase.ExecuteAsync(auditEvent.ToCommand(), cancellationToken);

            logger.LogInformation(
                "Kafka audit event persisted with status {Status}, auditSnapshotId {AuditSnapshotId}, durationMs {DurationMs}",
                "SUCCESS",
                snapshot.Id,
                stopwatch.ElapsedMilliseconds);

            consumer.Commit(result);
        }
        catch (Exception exception) when (exception is System.Text.Json.JsonException or ArgumentException)
        {
            logger.LogWarning(
                exception,
                "Kafka audit event rejected with status {Status}, errorCode {ErrorCode}, exceptionType {ExceptionType}, durationMs {DurationMs}",
                "VALIDATION_FAILED",
                "INVALID_AUDIT_EVENT",
                exception.GetType().Name,
                stopwatch.ElapsedMilliseconds);
            consumer.Commit(result);
        }
    }
}
