# .NET Refactoring und Clean Architecture

Dieses Dokument erklärt, was im .NET-Teil verbessert wurde.

Es geht nicht um perfekte Theorie. Es geht darum, den Code sauber, verständlich und wartbar zu machen.

## Ziel

Die .NET-Services sollten klarer aufgebaut werden.

Vorher waren einige Dinge stärker vermischt:

- viel Logik in `Program.cs`
- Endpunkte direkt im Startcode
- technische Details nah an der Business-Logik
- teilweise schwache Struktur für Logging und Persistenz

Jetzt ist die Struktur einfacher zu verstehen.

## Neue Grundstruktur

Die Services folgen jetzt diesem Aufbau:

```text
Api/
  Endpoints/
  DependencyInjection/
  Middleware und Requests

Application/
  Use Cases
  Interfaces
  DTOs

Domain/
  fachliche Modelle
  fachliche Regeln

Infrastructure/
  Persistence/
  Kafka/
  HTTP-Clients
  PDF/File Storage
  Observability

Program.cs
```

## Warum diese Aufteilung?

Der Vorteil dieser Lösung ist:

- Business-Logik liegt nicht in HTTP-Endpunkten.
- Use Cases können direkt getestet werden.
- Datenbank und Kafka bleiben technische Details.
- `Program.cs` bleibt kurz.
- Neue Funktionen lassen sich leichter ergänzen.

## Was wurde konkret verbessert?

### Program.cs

`Program.cs` ist jetzt hauptsächlich Composition Root.

Dort passiert:

- Services registrieren
- Logging konfigurieren
- Middleware aktivieren
- Endpoints mappen

Die eigentliche Arbeit liegt nicht mehr dort.

### Endpoints

REST-Endpunkte liegen unter:

```text
Api/Endpoints/
```

Die Endpoints sind dünn. Sie nehmen Requests an, prüfen einfache Fehler und rufen Use Cases auf.

### Use Cases

Die eigentlichen Abläufe liegen in `Application`.

Beispiele:

- `ReserveStockUseCase`
- `CreateInvoiceUseCase`
- `AppendAuditSnapshotUseCase`
- `GetAuditTimelineUseCase`

### Domain

Die Domain enthält fachliche Modelle.

Beispiele:

- `Reservation`
- `Invoice`
- `PaymentSucceeded`
- `AuditSnapshot`

Die Domain kennt keine Datenbank, kein Kafka und kein ASP.NET Core.

### Infrastructure

Infrastructure enthält technische Details.

Beispiele:

- EF Core DbContext
- PostgreSQL Repository
- Kafka Consumer
- PDF Writer
- HTTP Audit Sink
- OpenTelemetry-Anbindung

## Persistenz

Audit-Service:

- PostgreSQL
- EF Core
- Migrationen
- Repository: `EfAuditSnapshotRepository`

Warehouse-Service:

- PostgreSQL
- EF Core
- Migrationen
- Repository: `EfWarehouseRepository`
- Seed-Daten für Möbel-Produkte

Invoice-Service:

- PDF-Dateien
- JSON-Metadaten
- persistentes Docker Volume `invoice-data`

## Kafka

Kafka wurde sauber in `Infrastructure/Kafka` ergänzt.

Invoice konsumiert:

```text
PaymentSucceeded
```

Audit konsumiert:

```text
audit-events
```

Die Consumer enthalten nicht die Business-Logik. Sie rufen Use Cases auf.

## Logging

Alle Services schreiben strukturierte Logs.

Wichtige Felder:

- `service`
- `correlationId`
- `operation`
- `eventType`
- `status`
- `durationMs`

Das hilft beim Debugging in Grafana.

## Tests

Aktuell gibt es Tests für:

- Audit-Snapshots
- Audit-Timeline
- Warehouse-Reservierungen
- Inventory-Verwaltung
- Invoice-Erstellung
- Payment-Domain
- Kafka-Deserializer

## Was wurde bereits erledigt?

- Cleanere Ordnerstruktur.
- Program.cs stark vereinfacht.
- Endpoints ausgelagert.
- EF Core für Audit und Warehouse.
- Migrationen ergänzt.
- Kafka-Consumer für Invoice und Audit.
- Strukturierte Logs.
- Grafana/Loki/OpenTelemetry vorbereitet.
- PDF-Persistenz im Invoice-Service.
- Tests erweitert.

## Was ist noch offen?

- Gemeinsame Event-Contracts mit Java final abstimmen.
- Mehr End-to-End-Tests ergänzen.
- Grafana-Dashboards bauen.
- Admin UI später weiter absichern.
- Kafka Dead-Letter-Topic optional ergänzen.
