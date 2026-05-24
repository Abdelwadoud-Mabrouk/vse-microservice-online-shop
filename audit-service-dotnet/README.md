# Audit Service (.NET 10)

Dieser Service ist zuständig für die zentrale Audit-Historie.

Er speichert wichtige Ereignisse aus dem System. Dadurch kann man später nachvollziehen, was bei einer Bestellung passiert ist.

Beispiel:

```text
ORDER_CREATED
WAREHOUSE_RESERVED
PAYMENT_SUCCEEDED
INVOICE_CREATED
ORDER_COMPLETED
```

## Aufgabe des Service

Der Audit-Service speichert Audit-Snapshots dauerhaft in PostgreSQL.

Ein Snapshot ist ein einzelner Schritt im Bestellprozess. Jeder Snapshot enthält eine `correlationId`. Diese ID verbindet alle Events einer Bestellung.

Der Service ist bewusst append-only gedacht. Das bedeutet: Events werden hinzugefügt, aber nicht fachlich verändert. So bleibt die Historie nachvollziehbar.

## Kommunikation

Der Service kann Audit-Events auf zwei Wegen empfangen.

### REST

Andere Services können direkt ein Audit-Event senden:

```text
POST /audit/events
```

### Kafka

Der Service konsumiert zusätzlich Audit-Events aus Kafka.

Topic:

```text
audit-events
```

Der Kafka-Code liegt in:

```text
src/AuditService/Infrastructure/Kafka
```

Die Business-Logik liegt nicht im Kafka-Consumer. Der Consumer liest nur die Nachricht, prüft sie grob und ruft danach den Use Case auf.

## Endpunkte

```text
GET  /health
GET  /
GET  /admin
GET  /admin/service-health
POST /audit/events
GET  /audit/orders/{correlationId}
GET  /audit/orders
```

Wichtig:

- `GET /audit/orders/{correlationId}` zeigt die Timeline einer Bestellung.
- `GET /audit/orders` zeigt eine Zusammenfassung mehrerer Bestellungen.
- `/admin` öffnet die kleine Admin-Oberfläche.

## Beispiel für ein Audit-Event

```json
{
  "correlationId": "00000000-0000-0000-0000-000000000000",
  "eventType": "ORDER_CREATED",
  "service": "shop-service",
  "payload": {},
  "previousEventId": null,
  "actor": "shop-service",
  "statusCode": "SUCCESS"
}
```

## Datenbank

Der Service nutzt PostgreSQL.

Konfiguration:

```env
AUDIT_DB_CONNECTION=Host=audit-db;Port=5432;Database=...;Username=...;Password=...
```

Die Persistenz läuft über Entity Framework Core.

Wichtige Dateien:

```text
Infrastructure/Persistence/AuditDbContext.cs
Infrastructure/Persistence/Repositories/EfAuditSnapshotRepository.cs
Infrastructure/Persistence/Migrations/
```

## Kafka-Konfiguration

```env
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_AUDIT_EVENTS_TOPIC=audit-events
KAFKA_AUDIT_CONSUMER_GROUP=audit-service
```

Wenn `KAFKA_BOOTSTRAP_SERVERS` fehlt, läuft der Service weiter, aber der Kafka-Consumer ist deaktiviert.

## Logging

Der Service schreibt strukturierte Logs.

Wichtige Felder sind:

- `service`
- `correlationId`
- `operation`
- `eventType`
- `status`
- `durationMs`
- `errorCode`

Damit kann man in Grafana schnell prüfen, ob ein Event angekommen und gespeichert wurde.

## Lokal starten

Aus dem Repository-Root:

```bash
docker compose up -d --build audit-service
```

Oder direkt:

```bash
/usr/local/share/dotnet/dotnet run --project audit-service-dotnet/src/AuditService/AuditService.csproj
```

## Tests

```bash
/usr/local/share/dotnet/dotnet test audit-service-dotnet/tests/AuditService.Tests/AuditService.Tests.csproj
```

## Aktuell umgesetzt

- REST-Endpunkte für Audit-Events und Timeline-Abfragen.
- Kafka-Consumer für Audit-Events.
- PostgreSQL-Persistenz mit EF Core.
- Migrationen für das Datenbankschema.
- Strukturierte Logs.
- Admin UI mit Service-Status und Warehouse-Verwaltung.
- Tests für Use Cases und Kafka-Deserializer.

## Noch offen

- Event-Namen final mit dem Java-Team abstimmen.
- Grafana-Dashboard für Audit-Timelines bauen.
- Admin-Berechtigungen später noch stärker ausbauen.
