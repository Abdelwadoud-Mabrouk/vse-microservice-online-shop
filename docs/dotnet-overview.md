# .NET Überblick

Dieses Dokument erklärt den .NET-Teil des Projekts.

Es geht um diese Services:

- `warehouse-service-dotnet`
- `invoice-service-dotnet`
- `audit-service-dotnet`

Die Java-Services werden hier nur erwähnt, wenn es für das Verständnis wichtig ist.

## Grundidee

Jeder Service hat eine klare Aufgabe.

Der Vorteil dieser Lösung ist: Der Code bleibt verständlicher. Man kann einzelne Services ändern, testen und erweitern, ohne überall gleichzeitig arbeiten zu müssen.

## Die drei .NET-Services

### Warehouse Service

Dieser Service ist zuständig für Lagerbestand.

Er verwaltet:

- Produkte
- verfügbare Mengen
- Reservierungen
- Commit und Cancel von Reservierungen

Der Service wird per REST aufgerufen, weil der Shop direkt wissen muss, ob Ware verfügbar ist.

### Invoice Service

Dieser Service ist zuständig für Rechnungen.

Wenn eine Zahlung erfolgreich war, erstellt er eine PDF-Rechnung und speichert sie dauerhaft.

Der Service konsumiert `PaymentSucceeded` Events über Kafka. Zusätzlich gibt es einen REST-Endpunkt für Tests und Entwicklung.

### Audit Service

Dieser Service ist zuständig für die zentrale Historie.

Er speichert wichtige Events dauerhaft in PostgreSQL. Über die `correlationId` kann man später alle Schritte einer Bestellung anzeigen.

## Architektur der Services

Die .NET-Services sind pragmatisch nach Clean Architecture aufgebaut.

```text
Api/
  HTTP-Endpunkte, Requests, Middleware

Application/
  Use Cases, DTOs, Interfaces

Domain/
  fachliche Modelle und Regeln

Infrastructure/
  Datenbank, Kafka, PDF, HTTP-Clients, Logging-Anbindung

Program.cs
  Registrierung und Start der Anwendung
```

## Warum diese Aufteilung?

Die Business-Logik soll nicht in den HTTP-Endpunkten liegen.

Ein Endpoint nimmt nur eine Anfrage an und ruft einen Use Case auf.

Der Use Case enthält den eigentlichen Ablauf.

Die Infrastructure kümmert sich um Technik wie PostgreSQL, Kafka, PDF-Dateien oder HTTP-Clients.

Der Vorteil ist:

- Code ist leichter testbar.
- Änderungen an Datenbank oder Kafka ändern nicht direkt die Domain.
- Controller/Endpoints bleiben klein.
- Man findet schneller, wo ein Fehler entsteht.

## REST und Kafka

REST wird verwendet, wenn direkt eine Antwort gebraucht wird.

Beispiel:

```text
Shop -> Warehouse: Bitte reserviere diese Produkte.
Warehouse -> Shop: Reservierung erfolgreich oder nicht.
```

Kafka wird verwendet, wenn ein Schritt asynchron passieren kann.

Beispiel:

```text
Billing veröffentlicht PaymentSucceeded.
Invoice bekommt das Event später und erstellt die Rechnung.
```

## Datenhaltung

Audit-Service:

- PostgreSQL
- EF Core
- Tabelle für Audit-Snapshots

Warehouse-Service:

- PostgreSQL
- EF Core
- Tabellen für Produkte, Bestand und Reservierungen

Invoice-Service:

- Dateisystem
- PDF-Dateien
- JSON-Metadaten

## Logging und Observability

Alle .NET-Services schreiben strukturierte Logs.

Wichtige Felder:

- `service`
- `correlationId`
- `operation`
- `eventType`
- `status`
- `durationMs`

Die Logs können über OpenTelemetry an Loki und Grafana gesendet werden.

## Aktuell wurde bereits umgesetzt

- Saubere Trennung von API, Application, Domain und Infrastructure.
- EF Core für Audit und Warehouse.
- Persistente PostgreSQL-Datenbanken.
- PDF-Erzeugung im Invoice-Service.
- Kafka-Consumer für Invoice und Audit.
- Strukturierte Logs.
- Lokaler Grafana/Loki/OpenTelemetry Stack.
- Admin UI im Audit-Service.
- Unit Tests für wichtige Use Cases und Kafka-Deserializer.

## Noch offen ist

- Event-Contracts final mit dem Java-Team abstimmen.
- End-to-End-Tests über Java und .NET ergänzen.
- Grafana-Dashboards für Bestellungen bauen.
- Admin UI später stärker absichern.
- Kafka Topics in produktionsnaher Umgebung explizit verwalten.
