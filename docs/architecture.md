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

Auch zwischen Shop und Billing ist REST sinnvoll, weil der Shop eine direkte Zahlungsantwort braucht.

### Kafka

Kafka wird genutzt, wenn ein Schritt im Hintergrund passieren kann.

Beispiel:

```text
Shop -> Kafka -> Invoice
```

Nach einer abgeschlossenen Bestellung (`ORDER_COMPLETED`) erstellt der Invoice-Service die Rechnung asynchron.

### Kafka Topics

Topic-Namen orientieren sich an den Service-/Domänennamen. Ein Service publiziert seine Events in sein eigenes Topic. Die Event-Namen beschreiben das fachliche Ereignis.

| Topic | Publisher | Bedeutung |
| :--- | :--- | :--- |
| `orders` | Shop-Service | Events rund um den Bestellprozess |
| `billing` | Billing-Service | Events rund um Zahlungen |
| `warehouse` | Warehouse-Service | Events rund um Lager und Reservierungen |
| `invoices` | Invoice-Service | Events rund um Rechnungen |

Beispiel: Der Billing-Service publiziert in das Topic `billing`, aber die Events heißen fachlich `PAYMENT_INITIALIZED`, `PAYMENT_SUCCEEDED` und `PAYMENT_FAILED`.

Geplante Events pro Topic:

| Topic | Events |
| :--- | :--- |
| `orders` | `ORDER_CREATED`, `ORDER_COMPLETED`, `ORDER_CANCELLED`, `RESERVATION_ROLLBACK_REQUESTED` |
| `billing` | `PAYMENT_INITIALIZED`, `PAYMENT_SUCCEEDED`, `PAYMENT_FAILED` |
| `warehouse` | `WAREHOUSE_RESERVED`, `WAREHOUSE_COMMITTED`, `WAREHOUSE_RESERVATION_CANCELLED`, `WAREHOUSE_OUT_OF_STOCK` |
| `invoices` | `INVOICE_CREATION_STARTED`, `INVOICE_CREATED`, `INVOICE_CREATION_FAILED` |

Wichtige Consumer:

| Service | Konsumiert |
| :--- | :--- |
| Audit-Service | `orders`, `billing`, `warehouse`, `invoices` |
| Invoice-Service | `orders`, verarbeitet `ORDER_COMPLETED` |
| Warehouse-Service | später `orders`, für `ORDER_COMPLETED` und `RESERVATION_ROLLBACK_REQUESTED` |
| Shop-Service | `billing`, für `PAYMENT_SUCCEEDED` und `PAYMENT_FAILED` |

## Event-Idee

Für asynchrone Kommunikation wird ein Event-Envelope verwendet.

Ein Event enthält Metadaten und eine fachliche Payload.

Wichtige Felder sind:

- `eventId`
- `eventType`
- `source` oder `service`
- `correlationId`
- `timestamp`
- `payload`

Die `correlationId` verbindet alle Events einer Bestellung.

## Wichtige Event-Flows

Happy Path:

```text
OrderCreated
StockReserved
PaymentSucceeded
OrderCompleted
InvoiceCreated
StockCommitted
```

Fehlerfälle:

```text
StockReservationFailed
PaymentFailed
OrderCancelled
ReservationRollbackRequested
StockReleased
```

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

Der Audit-Service arbeitet append-only: Ereignisse werden hinzugefügt, aber fachlich nicht verändert.

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

## Datenbanken

Es wird das Prinzip "Database per Service" verwendet.

Aktuell wichtig:

- Shop-Service nutzt eine eigene PostgreSQL-Datenbank.
- Warehouse-Service nutzt eine eigene PostgreSQL-Datenbank.
- Audit-Service nutzt eine eigene PostgreSQL-Datenbank.
- Invoice-Service speichert PDF-Rechnungen und Metadaten im Dateisystem.

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
