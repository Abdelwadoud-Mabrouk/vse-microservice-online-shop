# Audit-Service

## Aufgabe

Der Audit-Service speichert die fachliche Historie einer Bestellung. Er zeigt
Ereignisse über die Correlation-ID als Timeline an.

## Technologie

- .NET 10
- ASP.NET Core Minimal APIs
- Entity Framework Core
- PostgreSQL
- Apache Kafka

## API

```text
GET  /health
POST /audit/events
GET  /audit/orders
GET  /audit/orders/{correlationId}
```

Die vollständige OpenAPI-Beschreibung ist im laufenden Docker-Stack unter
`http://localhost:5081/swagger` verfügbar.

## Kafka-Events

Der Service konsumiert die fachlichen Topics des Systems. Eine Event-ID wird
nur einmal gespeichert. Fehlt `previousEventId`, verwendet der Service den
letzten Eintrag derselben Correlation-ID.

## Datenbank

PostgreSQL speichert Audit-Snapshots append-only. Vorhandene fachliche Einträge
werden nicht überschrieben.

## Konfiguration

Wichtige Werte sind Datenbankverbindung, Kafka-Broker, konsumierte Topics,
Consumer-Gruppe und Logverzeichnis.

## Docker

```bash
docker compose up -d --build audit-service
```

## Tests

```bash
dotnet test audit-service-dotnet/tests/AuditService.Tests/AuditService.Tests.csproj
```
