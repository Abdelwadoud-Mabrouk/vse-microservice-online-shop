using System.Text.Json;
using InvoiceService.Infrastructure.Kafka;

namespace InvoiceService.Tests;

public sealed class OrderCompletedEventDeserializerTests
{
    [Fact]
    public void Deserialize_ReadsEnvelopeMessage()
    {
        var message = """
        {
          "eventId": "11111111-1111-4111-8111-111111111111",
          "correlationId": "22222222-2222-4222-8222-222222222222",
          "eventType": "ORDER_COMPLETED",
          "source": "shop-service",
          "timestamp": "2026-05-23T07:30:00Z",
          "payload": {
            "orderId": "33333333-3333-4333-8333-333333333333",
            "paymentId": "pay-test-001",
            "totalAmount": 199.99,
            "currency": "eur",
            "customerEmail": "customer@example.com",
            "customer": {
              "name": "Max Mustermann",
              "email": "customer@example.com",
              "street": "Musterstrasse 12",
              "postalCode": "58507",
              "city": "Luedenscheid",
              "country": "Deutschland"
            },
            "items": [
              {
                "sku": "SKU-CHAIR-OAK",
                "name": "Esszimmerstuhl Eiche",
                "quantity": 2,
                "unitPrice": 99.995
              }
            ]
          },
          "actor": "shop-service",
          "statusCode": "SUCCESS"
        }
        """;

        var orderEvent = OrderCompletedEventDeserializer.Deserialize(message);

        Assert.Equal(Guid.Parse("22222222-2222-4222-8222-222222222222"), orderEvent.CorrelationId);
        Assert.Equal(Guid.Parse("33333333-3333-4333-8333-333333333333"), orderEvent.OrderId);
        Assert.Equal("ORDER_COMPLETED", orderEvent.EventType);
        Assert.Equal("shop-service", orderEvent.Service);
        Assert.Equal("EUR", orderEvent.ToCommand(3).Payment.Currency);
        Assert.Single(orderEvent.Items);
    }

    [Fact]
    public void Deserialize_ReadsDirectPayloadMessage()
    {
        var message = """
        {
          "correlationId": "44444444-4444-4444-8444-444444444444",
          "orderId": "55555555-5555-4555-8555-555555555555",
          "transactionId": "tx-test-002",
          "amount": 42.50,
          "currency": "EUR",
          "customerEmail": "customer@example.com",
          "customer": {
            "name": "Max Mustermann",
            "email": "customer@example.com",
            "street": "Musterstrasse 12",
            "postalCode": "58507",
            "city": "Luedenscheid",
            "country": "Deutschland"
          },
          "items": [
            {
              "sku": "SKU-LAMP-BLACK",
              "name": "Stehlampe Schwarz",
              "quantity": 1,
              "unitPrice": 42.50
            }
          ]
        }
        """;

        var orderEvent = OrderCompletedEventDeserializer.Deserialize(message);

        Assert.Equal(Guid.Parse("44444444-4444-4444-8444-444444444444"), orderEvent.CorrelationId);
        Assert.Equal("ORDER_COMPLETED", orderEvent.EventType);
        Assert.Equal("shop-service", orderEvent.Service);
        Assert.Equal("tx-test-002", orderEvent.TransactionId);
    }

    [Fact]
    public void CanHandle_ReturnsFalseForOtherOrderEvents()
    {
        var message = """
        {
          "eventType": "ORDER_CREATED",
          "correlationId": "66666666-6666-4666-8666-666666666666"
        }
        """;

        Assert.False(OrderCompletedEventDeserializer.CanHandle(message));
    }

    [Fact]
    public void Deserialize_RejectsMalformedMessage()
    {
        Assert.ThrowsAny<JsonException>(() => OrderCompletedEventDeserializer.Deserialize("not-json"));
    }
}
