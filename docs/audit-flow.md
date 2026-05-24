# Audit Flow

Der Audit Flow zeigt, wie wichtige Ereignisse im System gespeichert werden.

Das Ziel ist einfach: Man soll später verstehen, was bei einer Bestellung passiert ist.

## Warum Audit?

Bei mehreren Services ist es schwer, einen Ablauf nur über einzelne Logs zu verstehen.

Der Audit-Service sammelt deshalb fachliche Ereignisse an einer zentralen Stelle.

Beispiel:

```text
ORDER_CREATED
WAREHOUSE_RESERVED
PAYMENT_SUCCEEDED
INVOICE_CREATED
ORDER_COMPLETED
```

## Die correlationId

Die `correlationId` verbindet alle Events einer Bestellung.

Ohne diese ID wäre es schwer, Zahlung, Lager und Rechnung einer Bestellung zusammenzubringen.

Beispiel:

```json
{
  "correlationId": "91000000-0000-4000-8000-000000000001",
  "eventType": "INVOICE_CREATED",
  "service": "invoice-service",
  "statusCode": "SUCCESS"
}
```

## Wie kommt ein Event in den Audit-Service?

Es gibt zwei Wege.

### REST

Ein Service sendet direkt:

```text
POST /audit/events
```

Das wird aktuell z.B. vom Invoice- und Warehouse-Service genutzt.

### Kafka

Der Audit-Service kann auch Kafka-Events konsumieren:

```text
Topic: audit-events
```

Der Kafka-Consumer ruft danach denselben Use Case auf wie der REST-Endpunkt.

Das ist wichtig, weil die fachliche Speicherung gleich bleibt.

## Was wird gespeichert?

Ein Audit-Snapshot enthält:

- `correlationId`
- `eventType`
- `service`
- `timestamp`
- `payload`
- `previousEventId`
- `actor`
- `statusCode`

Die Daten werden in PostgreSQL gespeichert.

## Timeline abfragen

Mit diesem Endpunkt sieht man alle Events zu einer Bestellung:

```text
GET /audit/orders/{correlationId}
```

Beispiel:

```text
GET /audit/orders/91000000-0000-4000-8000-000000000001
```

Für eine Übersicht gibt es:

```text
GET /audit/orders
```

## Warum append-only?

Audit-Daten sollen nachvollziehbar bleiben.

Darum werden Events hinzugefügt, aber fachlich nicht verändert.

Der Vorteil dieser Lösung ist:

- Man sieht die Historie.
- Fehler bleiben nachvollziehbar.
- Der Ablauf kann später erklärt werden.

## Zusammenhang mit Logging

Audit und Logging sind nicht dasselbe.

Audit:

- fachliche Historie
- langfristig gespeichert
- wichtig für Bestellungen und Transaktionen

Logging:

- technische Analyse
- wichtig für Debugging
- sichtbar in Grafana

Beides ergänzt sich.

## Aktuell umgesetzt

- Audit-Snapshots in PostgreSQL.
- REST-Endpunkt zum Speichern.
- Kafka-Consumer zum Speichern.
- Timeline-Abfrage über `correlationId`.
- Zusammenfassung aller Bestellungen.
- Strukturierte Logs beim Empfangen und Speichern.
- Admin UI für einfache Sicht auf den Zustand.

## Noch offen

- Einheitliche Event-Namen mit allen Services final festlegen.
- Optional eine schönere Timeline-Ansicht im Admin UI bauen.
- Später Filter nach Event-Typ oder Status ergänzen.
