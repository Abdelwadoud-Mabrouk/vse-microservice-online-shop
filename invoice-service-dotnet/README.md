# Invoice Service (.NET 10)

Creates invoices after successful payment and records every relevant step in the Audit Service.

## Architecture

- `Domain`: payment event, invoice, and invoice line items
- `Application`: invoice creation/get use cases plus repository/document/audit ports
- `Infrastructure`: in-memory invoice repository, JSON document writer, HTTP audit sink
- `Api`: payment event request, correlation middleware, and route mapping

## Endpoints

- `GET /health`
- `POST /events/payment-succeeded`
- `GET /invoices/{invoiceId}`

## PaymentSucceeded contract

```json
{
  "correlationId": "00000000-0000-0000-0000-000000000000",
  "orderId": "00000000-0000-0000-0000-000000000000",
  "transactionId": "tx-123",
  "amount": 129.99,
  "currency": "EUR",
  "customerEmail": "customer@example.com",
  "items": [
    {
      "sku": "SKU-CHAIR",
      "name": "Chair",
      "quantity": 1,
      "unitPrice": 129.99
    }
  ]
}
```

The current implementation exposes an HTTP event endpoint. The JSON contract can be reused by the Billing Service when RabbitMQ is connected.

## Configuration

- `AUDIT_SERVICE_URL`: Base URL of the Audit Service.
- `INVOICE_RETRY_COUNT`: Number of invoice generation attempts, default `3`.
- `INVOICE_FORCE_FAILURE`: Set to `true` to simulate invoice generation failure.
- `INVOICE_DATA_DIR`: Directory where generated invoice JSON files are stored.

## Local run

Requires .NET 10 SDK.

```bash
dotnet run --project src/InvoiceService/InvoiceService.csproj
```
