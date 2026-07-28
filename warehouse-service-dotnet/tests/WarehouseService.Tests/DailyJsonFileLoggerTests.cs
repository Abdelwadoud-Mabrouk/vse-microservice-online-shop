using System.Text.Json;
using Microsoft.Extensions.Logging;
using OnlineShop.Shared.Logging;

namespace WarehouseService.Tests;

public sealed class DailyJsonFileLoggerTests
{
    [Fact]
    public void Logger_WritesStructuredJsonAndRemovesExpiredFiles()
    {
        var directory = Path.Combine(Path.GetTempPath(), $"vse-log-test-{Guid.NewGuid():N}");
        Directory.CreateDirectory(directory);
        var expiredPath = Path.Combine(directory, "warehouse-service-2000-01-01.jsonl");
        File.WriteAllText(expiredPath, "{}");
        File.SetLastWriteTimeUtc(expiredPath, DateTime.UtcNow.AddDays(-20));

        using var provider = new DailyJsonFileLoggerProvider(directory, "warehouse-service", 14);
        var logger = provider.CreateLogger("WarehouseService.Tests");
        using (logger.BeginScope(new Dictionary<string, object?>
               {
                   ["correlationId"] = Guid.Parse("11111111-1111-4111-8111-111111111111")
               }))
        {
            logger.LogInformation("Reservation {ReservationId} created", "reservation-1");
            logger.LogError("Framework value {UnsupportedValue}", typeof(DailyJsonFileLoggerTests));
        }

        Assert.False(File.Exists(expiredPath));
        var currentPath = Directory.EnumerateFiles(directory, "warehouse-service-*.jsonl").Single();
        var lines = File.ReadAllLines(currentPath);
        Assert.Equal(2, lines.Length);
        using var document = JsonDocument.Parse(lines[0]);
        Assert.Equal("warehouse-service", document.RootElement.GetProperty("service").GetString());
        Assert.Equal("INFORMATION", document.RootElement.GetProperty("level").GetString());
        Assert.Equal(
            "11111111-1111-4111-8111-111111111111",
            document.RootElement.GetProperty("context").GetProperty("correlationId").GetString());
        Assert.Equal(
            "reservation-1",
            document.RootElement.GetProperty("context").GetProperty("ReservationId").GetString());
        using var frameworkLog = JsonDocument.Parse(lines[1]);
        Assert.Contains(
            nameof(DailyJsonFileLoggerTests),
            frameworkLog.RootElement.GetProperty("context").GetProperty("UnsupportedValue").GetString());
    }
}
