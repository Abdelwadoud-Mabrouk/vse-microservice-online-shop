using System.Text.Json;
using InvoiceService.Application;
using InvoiceService.Infrastructure.Kafka;

namespace InvoiceService.Tests;

public sealed class PaymentSucceededEventDeserializerTests
{
    [Fact]
    public void Deserialize_ReadsMinimalBillingEnvelope()
    {
        var message = """
        {
          "eventId": "11111111-1111-4111-8111-111111111111",
          "correlationId": "22222222-2222-4222-8222-222222222222",
          "eventType": "PAYMENT_SUCCEEDED",
          "service": "billing-service",
          "timestamp": "2026-05-23T07:30:00Z",
          "payload": {
            "paymentId": "77777777-7777-4777-8777-777777777777",
            "orderId": "33333333-3333-4333-8333-333333333333",
            "correlationId": "22222222-2222-4222-8222-222222222222",
            "amount": 199.99,
            "currency": "eur",
            "providerTransactionId": "provider-tx-001"
          },
          "actor": "billing-service",
          "statusCode": "SUCCESS"
        }
        """;

        var paymentEvent = PaymentSucceededEventDeserializer.Deserialize(message);

        Assert.Equal(Guid.Parse("22222222-2222-4222-8222-222222222222"), paymentEvent.CorrelationId);
        Assert.Equal(Guid.Parse("33333333-3333-4333-8333-333333333333"), paymentEvent.OrderId);
        Assert.Equal("PAYMENT_SUCCEEDED", paymentEvent.EventType);
        Assert.Equal("billing-service", paymentEvent.Service);
        Assert.Equal("provider-tx-001", paymentEvent.TransactionId);
        Assert.Equal("eur", paymentEvent.Currency);
        Assert.Null(paymentEvent.EmbeddedOrderDetails);
    }

    [Fact]
    public void Deserialize_ReadsBackwardCompatibleEmbeddedInvoiceDetails()
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

        var paymentEvent = PaymentSucceededEventDeserializer.Deserialize(message);
        var orderDetails = Assert.IsType<OrderInvoiceDetails>(paymentEvent.EmbeddedOrderDetails);
        var command = paymentEvent.ToCommand(orderDetails, 3);

        Assert.Equal(Guid.Parse("44444444-4444-4444-8444-444444444444"), paymentEvent.CorrelationId);
        Assert.Equal("PAYMENT_SUCCEEDED", paymentEvent.EventType);
        Assert.Equal("billing-service", paymentEvent.Service);
        Assert.Equal("tx-test-002", paymentEvent.TransactionId);
        Assert.Equal("EUR", command.Payment.Currency);
        Assert.Single(command.Payment.Items);
    }

    [Fact]
    public void CanHandle_ReturnsFalseForRefundEvents()
    {
        var message = """
        {
          "eventType": "REFUND_SUCCEEDED",
          "correlationId": "66666666-6666-4666-8666-666666666666"
        }
        """;

        Assert.False(PaymentSucceededEventDeserializer.CanHandle(message));
    }

    [Fact]
    public void Deserialize_RejectsMalformedMessage()
    {
        Assert.ThrowsAny<JsonException>(() => PaymentSucceededEventDeserializer.Deserialize("not-json"));
    }
}
