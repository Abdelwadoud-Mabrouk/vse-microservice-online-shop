using System.Text.Json;
using AuditService.Infrastructure.Kafka;

namespace AuditService.Tests;

public sealed class AuditEventEnvelopeDeserializerTests
{
    [Fact]
    public void Deserialize_ReadsAuditEventEnvelope()
    {
        var message = """
        {
          "eventId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
          "correlationId": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
          "eventType": "WAREHOUSE_RESERVED",
          "service": "warehouse-service",
          "timestamp": "2026-05-23T07:30:00Z",
          "payload": {
            "reservationId": "res-123"
          },
          "actor": "warehouse-service",
          "statusCode": "SUCCESS"
        }
        """;

        var envelope = AuditEventEnvelopeDeserializer.Deserialize(message);

        Assert.Equal(Guid.Parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"), envelope.EventId);
        Assert.Equal(Guid.Parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), envelope.CorrelationId);
        Assert.Equal("WAREHOUSE_RESERVED", envelope.EventType);
        Assert.Equal("warehouse-service", envelope.Service);
        Assert.Equal("SUCCESS", envelope.StatusCode);
        Assert.True(envelope.Payload.ContainsKey("reservationId"));
    }

    [Fact]
    public void Deserialize_UsesSafeDefaultsForOptionalFields()
    {
        var message = """
        {
          "correlationId": "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
          "eventType": "INVOICE_CREATED",
          "service": "invoice-service"
        }
        """;

        var envelope = AuditEventEnvelopeDeserializer.Deserialize(message);

        Assert.NotEqual(Guid.Empty, envelope.EventId);
        Assert.Equal("invoice-service", envelope.Actor);
        Assert.Equal("SUCCESS", envelope.StatusCode);
        Assert.Empty(envelope.Payload);
    }

    [Fact]
    public void Deserialize_RejectsMissingCorrelationId()
    {
        var message = """
        {
          "eventType": "INVOICE_CREATED",
          "service": "invoice-service"
        }
        """;

        Assert.Throws<JsonException>(() => AuditEventEnvelopeDeserializer.Deserialize(message));
    }
}
