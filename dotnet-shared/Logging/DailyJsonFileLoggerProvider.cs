using System.Collections;
using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;

namespace OnlineShop.Shared.Logging;

public static class DailyJsonFileLoggingExtensions
{
    public static ILoggingBuilder AddDailyJsonFile(
        this ILoggingBuilder logging,
        IConfiguration configuration,
        string contentRootPath,
        string serviceName)
    {
        var directory = configuration["LOG_DIRECTORY"];
        if (string.IsNullOrWhiteSpace(directory))
        {
            directory = Path.Combine(contentRootPath, "logs");
        }

        var retentionDays = int.TryParse(configuration["LOG_RETENTION_DAYS"], out var configuredDays)
            ? Math.Max(1, configuredDays)
            : 14;

        logging.AddProvider(new DailyJsonFileLoggerProvider(directory, serviceName, retentionDays));
        return logging;
    }
}

public sealed class DailyJsonFileLoggerProvider : ILoggerProvider, ISupportExternalScope
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private readonly object _sync = new();
    private readonly string _directory;
    private readonly string _serviceName;
    private readonly int _retentionDays;
    private IExternalScopeProvider _scopes = new LoggerExternalScopeProvider();

    public DailyJsonFileLoggerProvider(string directory, string serviceName, int retentionDays)
    {
        _directory = directory;
        _serviceName = serviceName;
        _retentionDays = retentionDays;
        Directory.CreateDirectory(_directory);
        DeleteExpiredFiles();
    }

    public ILogger CreateLogger(string categoryName) => new DailyJsonFileLogger(this, categoryName);

    public void SetScopeProvider(IExternalScopeProvider scopeProvider) => _scopes = scopeProvider;

    public void Dispose()
    {
    }

    private void Write<TState>(
        string category,
        LogLevel level,
        EventId eventId,
        TState state,
        Exception? exception,
        Func<TState, Exception?, string> formatter)
    {
        var context = new Dictionary<string, object?>(StringComparer.OrdinalIgnoreCase);
        AddProperties(context, state);
        _scopes.ForEachScope(
            static (scope, target) => AddProperties(target, scope),
            context);

        context.Remove("{OriginalFormat}");
        var entry = new Dictionary<string, object?>
        {
            ["timestamp"] = DateTimeOffset.UtcNow,
            ["service"] = _serviceName,
            ["level"] = level.ToString().ToUpperInvariant(),
            ["category"] = category,
            ["eventId"] = eventId.Id == 0 ? null : eventId.Id,
            ["message"] = formatter(state, exception),
            ["context"] = context.Count == 0 ? null : context,
            ["exceptionType"] = exception?.GetType().Name,
            ["exception"] = exception?.ToString()
        };

        string json;
        try
        {
            json = JsonSerializer.Serialize(entry, JsonOptions);
        }
        catch (Exception serializationException)
        {
            json = JsonSerializer.Serialize(new
            {
                timestamp = DateTimeOffset.UtcNow,
                service = _serviceName,
                level = level.ToString().ToUpperInvariant(),
                category,
                message = formatter(state, exception),
                loggingError = serializationException.Message,
                exception = exception?.ToString()
            }, JsonOptions);
        }
        var path = Path.Combine(_directory, $"{_serviceName}-{DateTime.UtcNow:yyyy-MM-dd}.jsonl");
        lock (_sync)
        {
            File.AppendAllText(path, json + Environment.NewLine);
        }
    }

    private void DeleteExpiredFiles()
    {
        var oldestDate = DateTime.UtcNow.Date.AddDays(-(_retentionDays - 1));
        foreach (var path in Directory.EnumerateFiles(_directory, $"{_serviceName}-*.jsonl"))
        {
            if (File.GetLastWriteTimeUtc(path) < oldestDate)
            {
                File.Delete(path);
            }
        }
    }

    private static void AddProperties(IDictionary<string, object?> target, object? state)
    {
        if (state is IEnumerable<KeyValuePair<string, object?>> properties)
        {
            foreach (var property in properties)
            {
                target[property.Key] = ToJsonSafeValue(property.Value);
            }
            return;
        }

        if (state is IEnumerable sequence && state is not string)
        {
            foreach (var item in sequence)
            {
                AddProperties(target, item);
            }
        }
    }

    private static object? ToJsonSafeValue(object? value)
    {
        return value switch
        {
            null => null,
            string or bool or byte or sbyte or short or ushort or int or uint or long or ulong
                or float or double or decimal or Guid or DateTime or DateTimeOffset or JsonElement => value,
            Enum enumValue => enumValue.ToString(),
            Type type => type.FullName ?? type.Name,
            _ => value.ToString()
        };
    }

    private sealed class DailyJsonFileLogger(
        DailyJsonFileLoggerProvider provider,
        string category) : ILogger
    {
        public IDisposable? BeginScope<TState>(TState state) where TState : notnull =>
            provider._scopes.Push(state);

        public bool IsEnabled(LogLevel logLevel) => logLevel != LogLevel.None;

        public void Log<TState>(
            LogLevel logLevel,
            EventId eventId,
            TState state,
            Exception? exception,
            Func<TState, Exception?, string> formatter)
        {
            if (IsEnabled(logLevel))
            {
                provider.Write(category, logLevel, eventId, state, exception, formatter);
            }
        }
    }
}
