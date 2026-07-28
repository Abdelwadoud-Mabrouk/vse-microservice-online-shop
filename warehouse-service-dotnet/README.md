# Warehouse-Service

## Aufgabe

Der Warehouse-Service verwaltet Bestand und Reservierungen. Er reserviert
Artikel, bestätigt den endgültigen Lagerabgang oder gibt eine Reservierung frei.

## Technologie

- .NET 10
- ASP.NET Core Minimal APIs
- Entity Framework Core
- PostgreSQL

## API

```text
GET    /health
GET    /inventory
GET    /inventory/{productId}
POST   /inventory
PUT    /inventory/{productId}
PATCH  /inventory/{productId}/stock
DELETE /inventory/{productId}
POST   /reservations
POST   /reservations/{reservationId}/commit
DELETE /reservations/{reservationId}
```

Die vollständige OpenAPI-Beschreibung ist im laufenden Docker-Stack unter
`http://localhost:5082/swagger` verfügbar. Eine statische Fassung liegt unter
`docs/openapi/warehouse-service.yaml`.

## Kafka-Events

Der Service veröffentlicht unter anderem:

- `WAREHOUSE_RESERVED`
- `WAREHOUSE_COMMITTED`
- `WAREHOUSE_CANCELLED`

## Datenbank

PostgreSQL speichert Produkte, Bestand und Reservierungen. `orderId` verhindert
doppelte Reservierungen. Datenbanksperren schützen parallele Zugriffe.

## Konfiguration

Wichtige Werte sind Datenbankverbindung, Audit-URL, Logverzeichnis und
Correlation-ID. Für einen Refund-Test kann der Commit in Development
kontrolliert fehlschlagen:

```env
FailureSimulation__FailCommit=true
```

## Docker

```bash
docker compose up -d --build warehouse-service
```

## Tests

```bash
dotnet test warehouse-service-dotnet/tests/WarehouseService.Tests/WarehouseService.Tests.csproj
```
