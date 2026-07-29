using System.Text.Json;
using InvoiceService.Application;
using InvoiceService.Application.Events;
using InvoiceService.Domain;

namespace InvoiceService.Infrastructure.Kafka;

public static class PaymentSucceededEventDeserializer
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true
    };

    public static bool CanHandle(string message)
    {
        using var document = JsonDocument.Parse(message);
        var root = document.RootElement;
        return string.Equals(ReadString(root, "eventType"), "PAYMENT_SUCCEEDED", StringComparison.OrdinalIgnoreCase);
    }

    public static PaymentSucceededEvent Deserialize(string message)
    {
        using var document = JsonDocument.Parse(message);
        var root = document.RootElement;
        var payload = root.TryGetProperty("payload", out var payloadElement)
            ? payloadElement
            : root;

        var correlationId = ReadGuid(root, "correlationId", required: false)
            ?? ReadGuid(payload, "correlationId", required: false)
            ?? Guid.Empty;

        var eventType = ReadString(root, "eventType") ?? "PAYMENT_SUCCEEDED";
        if (!string.Equals(eventType, "PAYMENT_SUCCEEDED", StringComparison.OrdinalIgnoreCase))
        {
            throw new JsonException("Invoice service only handles PAYMENT_SUCCEEDED events.");
        }

        return new PaymentSucceededEvent(
            ReadGuid(root, "eventId", required: false) ?? Guid.NewGuid(),
            correlationId,
            eventType,
            ReadString(root, "service") ?? ReadString(root, "source") ?? "billing-service",
            ReadDateTime(root, "timestamp") ?? ReadDateTime(root, "occurredAt") ?? DateTimeOffset.UtcNow,
            ReadGuid(payload, "orderId") ?? Guid.Empty,
            ReadString(payload, "providerTransactionId")
                ?? ReadString(payload, "transactionId")
                ?? ReadString(payload, "paymentId")
                ?? string.Empty,
            ReadDecimal(payload, "amount", "totalAmount"),
            ReadString(payload, "currency") ?? string.Empty,
            TryReadOrderDetails(payload),
            ReadGuid(root, "previousEventId", required: false),
            ReadString(root, "actor") ?? ReadString(root, "service") ?? ReadString(root, "source") ?? "billing-service",
            ReadString(root, "statusCode") ?? "SUCCESS");
    }

    private static OrderInvoiceDetails? TryReadOrderDetails(JsonElement payload)
    {
        if (!payload.TryGetProperty("customer", out var customer)
            || !payload.TryGetProperty("items", out var items)
            || items.ValueKind != JsonValueKind.Array)
        {
            return null;
        }

        var email = ReadString(customer, "email")
            ?? ReadString(payload, "customerEmail")
            ?? string.Empty;
        var customerDetails = CustomerDetails.Create(
            ReadString(customer, "name") ?? string.Empty,
            email,
            ReadString(customer, "street") ?? string.Empty,
            ReadString(customer, "postalCode") ?? string.Empty,
            ReadString(customer, "city") ?? string.Empty,
            ReadString(customer, "country") ?? string.Empty);
        var invoiceItems = items.EnumerateArray()
            .Select(item => InvoiceLineItem.Create(
                ReadString(item, "sku") ?? string.Empty,
                ReadString(item, "name") ?? string.Empty,
                ReadInt(item, "quantity"),
                ReadDecimal(item, "unitPrice")))
            .ToArray();

        return new OrderInvoiceDetails(email, customerDetails, invoiceItems);
    }

    private static Guid? ReadGuid(JsonElement element, string name, bool required = true)
    {
        if (!element.TryGetProperty(name, out var value) || value.ValueKind == JsonValueKind.Null)
        {
            if (required)
            {
                throw new JsonException($"{name} is required.");
            }

            return null;
        }

        if (value.ValueKind == JsonValueKind.String && Guid.TryParse(value.GetString(), out var guid))
        {
            return guid;
        }

        throw new JsonException($"{name} must be a GUID.");
    }

    private static string? ReadString(JsonElement element, string name)
    {
        return element.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;
    }

    private static decimal ReadDecimal(JsonElement element, params string[] names)
    {
        foreach (var name in names)
        {
            if (element.TryGetProperty(name, out var value) && value.TryGetDecimal(out var result))
            {
                return result;
            }
        }

        throw new JsonException($"{string.Join(" or ", names)} must be a decimal number.");
    }

    private static int ReadInt(JsonElement element, string name)
    {
        if (!element.TryGetProperty(name, out var value) || !value.TryGetInt32(out var result))
        {
            throw new JsonException($"{name} must be an integer.");
        }

        return result;
    }

    private static DateTimeOffset? ReadDateTime(JsonElement element, string name)
    {
        if (!element.TryGetProperty(name, out var value) || value.ValueKind == JsonValueKind.Null)
        {
            return null;
        }

        return value.Deserialize<DateTimeOffset?>(JsonOptions);
    }
}
