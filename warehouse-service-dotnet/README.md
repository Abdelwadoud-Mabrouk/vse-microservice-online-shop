# Warehouse Service (.NET 10)

Manages stock reservation, cancellation, and final stock commit for orders.

## Architecture

- `Domain`: reservation entity, items, and reservation status
- `Application`: reserve, cancel, commit, inventory use cases and ports
- `Infrastructure`: in-memory warehouse repository and HTTP audit sink
- `Api`: request model, correlation middleware, and route mapping

## Endpoints

- `GET /health`
- `GET /inventory`
- `POST /reservations`
- `DELETE /reservations/{reservationId}`
- `POST /reservations/{reservationId}/commit`

## Reservation request

```json
{
  "orderId": "00000000-0000-0000-0000-000000000000",
  "items": [
    {
      "sku": "SKU-CHAIR",
      "quantity": 1
    }
  ]
}
```

Send `X-Correlation-Id` with every request. If the header is missing, the service creates one.

## Configuration

- `AUDIT_SERVICE_URL`: Base URL of the Audit Service.
- `WAREHOUSE_INITIAL_STOCK`: Comma-separated stock config, for example `SKU-CHAIR:10,SKU-DESK:5`.

## Local run

Requires .NET 10 SDK.

```bash
dotnet run --project src/WarehouseService/WarehouseService.csproj
```
