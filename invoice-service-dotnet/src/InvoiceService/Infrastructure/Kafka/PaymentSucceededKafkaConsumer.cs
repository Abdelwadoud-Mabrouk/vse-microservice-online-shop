using System.Diagnostics;
using Confluent.Kafka;
using InvoiceService.Application;

namespace InvoiceService.Infrastructure.Kafka;

public sealed class PaymentSucceededKafkaConsumer(
    IServiceScopeFactory scopeFactory,
    KafkaConsumerOptions options,
    ILogger<PaymentSucceededKafkaConsumer> logger,
    IConfiguration configuration) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!options.Enabled)
        {
            logger.LogInformation(
                "Kafka consumer disabled for operation {Operation}. Set KAFKA_BOOTSTRAP_SERVERS to enable it.",
                "invoice.kafka.payment_succeeded.consume");
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
        consumer.Subscribe(options.PaymentSucceededTopic);

        logger.LogInformation(
            "Kafka consumer subscribed to topic {Topic} with group {ConsumerGroupId}",
            options.PaymentSucceededTopic,
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
        var operation = "invoice.kafka.payment_succeeded.consume";

        logger.LogInformation(
            "Kafka message received from topic {Topic}, partition {Partition}, offset {Offset}",
            result.Topic,
            result.Partition.Value,
            result.Offset.Value);

        try
        {
            var paymentEvent = PaymentSucceededEventDeserializer.Deserialize(result.Message.Value);
            using var scope = logger.BeginScope(new Dictionary<string, object?>
            {
                ["service"] = "invoice-service",
                ["correlationId"] = paymentEvent.CorrelationId,
                ["operation"] = operation,
                ["eventType"] = paymentEvent.EventType,
                ["orderId"] = paymentEvent.OrderId
            });

            logger.LogInformation(
                "Kafka message deserialized for event {EventType} from {SourceService}",
                paymentEvent.EventType,
                paymentEvent.Service);

            var retryCount = int.TryParse(configuration["INVOICE_RETRY_COUNT"], out var configuredRetryCount)
                ? configuredRetryCount
                : 3;

            using var serviceScope = scopeFactory.CreateScope();
            var useCase = serviceScope.ServiceProvider.GetRequiredService<CreateInvoiceUseCase>();
            var invoiceResult = await useCase.ExecuteAsync(paymentEvent.ToCommand(retryCount), cancellationToken);

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
            logger.LogWarning(
                exception,
                "Kafka message rejected with status {Status}, errorCode {ErrorCode}, exceptionType {ExceptionType}, durationMs {DurationMs}",
                "VALIDATION_FAILED",
                "INVALID_PAYMENT_SUCCEEDED_EVENT",
                exception.GetType().Name,
                stopwatch.ElapsedMilliseconds);
            consumer.Commit(result);
        }
    }
}
