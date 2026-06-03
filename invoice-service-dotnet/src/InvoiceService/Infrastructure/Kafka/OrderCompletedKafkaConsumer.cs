using System.Diagnostics;
using System.Text.Json;
using Confluent.Kafka;
using InvoiceService.Application;

namespace InvoiceService.Infrastructure.Kafka;

public sealed class OrderCompletedKafkaConsumer(
    IServiceScopeFactory scopeFactory,
    KafkaConsumerOptions options,
    ILogger<OrderCompletedKafkaConsumer> logger,
    IConfiguration configuration) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!options.Enabled)
        {
            logger.LogInformation(
                "Kafka consumer disabled for operation {Operation}. Set KAFKA_BOOTSTRAP_SERVERS to enable it.",
                "invoice.kafka.order_completed.consume");
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
        consumer.Subscribe(options.OrdersTopic);

        logger.LogInformation(
            "Kafka consumer subscribed to topic {Topic} with group {ConsumerGroupId}",
            options.OrdersTopic,
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
        var operation = "invoice.kafka.order_completed.consume";

        logger.LogInformation(
            "Kafka message received from topic {Topic}, partition {Partition}, offset {Offset}",
            result.Topic,
            result.Partition.Value,
            result.Offset.Value);

        try
        {
            if (!OrderCompletedEventDeserializer.CanHandle(result.Message.Value))
            {
                logger.LogDebug(
                    "Kafka message ignored because eventType is not ORDER_COMPLETED. topic {Topic}, partition {Partition}, offset {Offset}",
                    result.Topic,
                    result.Partition.Value,
                    result.Offset.Value);
                consumer.Commit(result);
                return;
            }

            var orderEvent = OrderCompletedEventDeserializer.Deserialize(result.Message.Value);
            using var scope = logger.BeginScope(new Dictionary<string, object?>
            {
                ["service"] = "invoice-service",
                ["correlationId"] = orderEvent.CorrelationId,
                ["operation"] = operation,
                ["eventType"] = orderEvent.EventType,
                ["orderId"] = orderEvent.OrderId
            });

            logger.LogInformation(
                "Kafka message deserialized for event {EventType} from {SourceService}",
                orderEvent.EventType,
                orderEvent.Service);

            var retryCount = int.TryParse(configuration["INVOICE_RETRY_COUNT"], out var configuredRetryCount)
                ? configuredRetryCount
                : 3;

            using var serviceScope = scopeFactory.CreateScope();
            var useCase = serviceScope.ServiceProvider.GetRequiredService<CreateInvoiceUseCase>();
            var invoiceResult = await useCase.ExecuteAsync(orderEvent.ToCommand(retryCount), cancellationToken);

            if (!invoiceResult.Success)
            {
                logger.LogWarning(
                    "Kafka workflow failed with status {Status}, errorCode {ErrorCode}, attempts {Attempts}, reason {Reason}, durationMs {DurationMs}",
                    "BUSINESS_FAILED",
                    "INVOICE_CREATION_FAILED",
                    invoiceResult.Attempts,
                    invoiceResult.Message,
                    stopwatch.ElapsedMilliseconds);
                consumer.Commit(result);
                return;
            }

            logger.LogInformation(
                "Kafka workflow completed with status {Status}, invoiceId {InvoiceId}, durationMs {DurationMs}",
                "SUCCESS",
                invoiceResult.Invoice!.InvoiceId,
                stopwatch.ElapsedMilliseconds);

            consumer.Commit(result);
        }
        catch (Exception exception) when (exception is System.Text.Json.JsonException or ArgumentException)
        {
            await RecordRejectedOrderCompletedAsync(result.Message.Value, exception, cancellationToken);

            logger.LogWarning(
                exception,
                "Kafka message rejected with status {Status}, errorCode {ErrorCode}, exceptionType {ExceptionType}, durationMs {DurationMs}",
                "VALIDATION_FAILED",
                "INVALID_ORDER_COMPLETED_EVENT",
                exception.GetType().Name,
                stopwatch.ElapsedMilliseconds);
            consumer.Commit(result);
        }
    }

    private async Task RecordRejectedOrderCompletedAsync(
        string message,
        Exception exception,
        CancellationToken cancellationToken)
    {
        if (!TryReadRejectedOrderCompleted(message, out var rejectedEvent))
        {
            return;
        }

        using var serviceScope = scopeFactory.CreateScope();
        var audit = serviceScope.ServiceProvider.GetRequiredService<IAuditSink>();
        await audit.RecordAsync(
            rejectedEvent.CorrelationId,
            "INVOICE_CREATION_FAILED",
            "FAILURE",
            new
            {
                rejectedEvent.OrderId,
                reason = exception.Message,
                errorCode = "INVALID_ORDER_COMPLETED_EVENT",
                sourceEventType = "ORDER_COMPLETED",
                sourceService = rejectedEvent.Service
            },
            cancellationToken);
    }

    private static bool TryReadRejectedOrderCompleted(string message, out RejectedOrderCompletedEvent rejectedEvent)
    {
        rejectedEvent = default;

        try
        {
            using var document = JsonDocument.Parse(message);
            var root = document.RootElement;
            if (!string.Equals(ReadString(root, "eventType"), "ORDER_COMPLETED", StringComparison.OrdinalIgnoreCase))
            {
                return false;
            }

            var payload = root.TryGetProperty("payload", out var payloadElement)
                ? payloadElement
                : root;
            var correlationId = ReadGuid(root, "correlationId")
                ?? ReadGuid(payload, "correlationId");
            if (correlationId is null)
            {
                return false;
            }

            rejectedEvent = new RejectedOrderCompletedEvent(
                correlationId.Value,
                ReadGuid(payload, "orderId"),
                ReadString(root, "service") ?? ReadString(root, "source") ?? "shop-service");
            return true;
        }
        catch (JsonException)
        {
            return false;
        }
    }

    private static Guid? ReadGuid(JsonElement element, string name)
    {
        return element.TryGetProperty(name, out var value)
            && value.ValueKind == JsonValueKind.String
            && Guid.TryParse(value.GetString(), out var guid)
            ? guid
            : null;
    }

    private static string? ReadString(JsonElement element, string name)
    {
        return element.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;
    }

    private readonly record struct RejectedOrderCompletedEvent(
        Guid CorrelationId,
        Guid? OrderId,
        string Service);
}
