# Architekturübersicht

Dieses Dokument erklärt die Architektur des Online-Shop-Projekts einfach und praktisch.

Das System besteht aus mehreren Services. Jeder Service hat eine eigene Aufgabe.

## Ziel des Systems

Ein Kunde soll eine Bestellung auslösen können.

Dabei passieren mehrere Schritte:

1. Bestellung wird angenommen.
2. Lagerbestand wird reserviert.
3. Zahlung wird durchgeführt.
4. Rechnung wird erstellt.
5. Wichtige Schritte werden im Audit gespeichert.

## Services

Java-Services:

- `shop-service-java`: Einstiegspunkt für Bestellungen und Orchestrierung.
- `billing-service-java`: Zahlung und Payment-Fassade.

.NET-Services:

- `warehouse-service-dotnet`: Lagerbestand und Reservierungen.
- `invoice-service-dotnet`: Rechnungserstellung.
- `audit-service-dotnet`: zentrale Audit-Historie.

## Kommunikation

Das System nutzt REST und Kafka.

### REST

REST wird genutzt, wenn direkt eine Antwort gebraucht wird.

Beispiel:

```text
Shop -> Warehouse
```

Der Shop muss direkt wissen, ob Produkte reserviert werden konnten.

### Kafka

Kafka wird genutzt, wenn ein Schritt im Hintergrund passieren kann.

Beispiel:

```text
Billing -> Kafka -> Invoice
```

Nach einer erfolgreichen Zahlung erstellt der Invoice-Service die Rechnung asynchron.

## Warum diese Architektur?

Der Vorteil dieser Lösung ist:

- Jeder Service bleibt fachlich überschaubar.
- Warehouse, Invoice und Audit können getrennt getestet werden.
- Fehler lassen sich über Logs und Audit besser nachvollziehen.
- Kafka entkoppelt Hintergrundprozesse.
- REST bleibt dort, wo direkte Antworten nötig sind.

## Audit

Der Audit-Service speichert wichtige Ereignisse dauerhaft.

Jedes Event enthält eine `correlationId`. Damit kann man alle Schritte einer Bestellung zusammen anzeigen.

Beispiel:

```text
ORDER_CREATED
WAREHOUSE_RESERVED
PAYMENT_SUCCEEDED
INVOICE_CREATED
ORDER_COMPLETED
```

## Logging und Grafana

Die .NET-Services schreiben strukturierte Logs.

Diese Logs können in Grafana angezeigt werden.

Damit kann man z.B. suchen:

```text
correlationId="..."
status="FAILURE"
eventType="PaymentSucceeded"
```

Das hilft beim Debugging und bei der Fehlersuche.

## Wichtige Dokumente

- [Systemdiagramme](system-diagrams.md)
- [.NET Überblick](dotnet-overview.md)
- [.NET Diagramme](dotnet-services-diagrams.md)
- [Kafka im .NET-Teil](kafka-integration-dotnet.md)
- [Audit Flow](audit-flow.md)
- [Logging und Observability](logging-observability.md)

## Aktueller Stand

Aktuell wurde bereits umgesetzt:

- .NET Services mit sauberer Struktur.
- Warehouse mit PostgreSQL.
- Audit mit PostgreSQL.
- Invoice mit PDF-Dateien und Metadaten.
- Kafka für Invoice und Audit.
- REST für Warehouse.
- Strukturierte Logs.
- Grafana/Loki/OpenTelemetry lokal.
- Tests für wichtige Teile.

Noch offen:

- Java- und .NET-Events final abstimmen.
- Mehr End-to-End-Tests bauen.
- Grafana-Dashboards ergänzen.
- Admin UI später weiter verbessern.
