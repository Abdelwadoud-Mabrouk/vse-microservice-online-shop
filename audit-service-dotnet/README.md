# Audit Service (.NET 10)

Stores immutable audit snapshots for all order-related domain events.

## Architecture

- `Domain`: `AuditSnapshot`
- `Application`: append timeline use cases and `IAuditSnapshotRepository`
- `Infrastructure`: PostgreSQL implementation
- `Api`: request model and HTTP mapping in `Program.cs`

## Endpoints

- `GET /health`
- `POST /audit/events`
- `GET /audit/orders/{correlationId}`

## Snapshot contract

```json
{
  "correlationId": "00000000-0000-0000-0000-000000000000",
  "eventType": "ORDER_PLACED",
  "service": "shop-service",
  "payload": {},
  "previousEventId": null,
  "actor": "shop-service",
  "statusCode": "SUCCESS"
}
```

## Configuration

- `AUDIT_DB_CONNECTION`: PostgreSQL connection string.

## Local run

Requires .NET 10 SDK.

```bash
dotnet run --project src/AuditService/AuditService.csproj
```
