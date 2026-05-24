# Logging und Observability

Dieses Dokument erklärt, wie Logging im .NET-Teil gedacht ist.

Das Ziel ist nicht, möglichst viele Logs zu schreiben.

Das Ziel ist: gute Logs schreiben, die beim Debugging wirklich helfen.

## Warum strukturierte Logs?

Normale Textlogs sind schwer zu filtern.

Strukturierte Logs enthalten Felder.

Beispiel:

```text
service=invoice-service
correlationId=91000000-0000-4000-8000-000000000001
operation=invoice.create
eventType=PaymentSucceeded
status=SUCCESS
durationMs=120
```

Damit kann man in Grafana gezielt suchen.

## Wichtige Felder

Die .NET-Services loggen besonders diese Felder:

- `service`
- `correlationId`
- `operation`
- `route`
- `eventType`
- `orderId`
- `entityId`
- `status`
- `durationMs`
- `errorCode`
- `exceptionType`

## Was wird geloggt?

Es werden nur wichtige Schritte geloggt.

Beispiele:

- REST-Anfrage für Reservierung kommt an.
- Warehouse-Reservierung war erfolgreich.
- Bestand reicht nicht aus.
- Kafka-Nachricht wurde empfangen.
- Kafka-Nachricht konnte nicht gelesen werden.
- Rechnungserstellung wurde gestartet.
- Rechnung wurde gespeichert.
- Audit-Event wurde gespeichert.

Framework-Lärm von `Microsoft.*` und `System.*` wird reduziert.

## Grafana, Loki und OpenTelemetry

Der lokale Observability-Stack besteht aus:

- OpenTelemetry Collector
- Loki
- Grafana

Der Ablauf ist:

```text
.NET Service
  -> OpenTelemetry Collector
  -> Loki
  -> Grafana
```

Grafana zeigt die Logs an. Loki speichert und durchsucht sie.

## Lokaler Start

```bash
docker compose up -d loki otel-collector grafana
```

Mit den .NET-Services:

```bash
docker compose up -d --build audit-service warehouse-service invoice-service loki otel-collector grafana
```

Grafana:

```text
http://localhost:3000
```

Die Zugangsdaten stehen in der `.env`.

## Wie debuggt man einen Fehler?

Praktischer Ablauf:

1. `correlationId` aus Request, Audit oder UI nehmen.
2. In Grafana nach dieser ID suchen.
3. Prüfen, welcher Service zuletzt erfolgreich war.
4. Nach `status=FAILURE` oder `status=VALIDATION_FAILED` filtern.
5. `errorCode` und `exceptionType` ansehen.

Beispiel:

```text
correlationId="91000000-0000-4000-8000-000000000001"
```

So sieht man den Ablauf über Warehouse, Invoice und Audit hinweg.

## Typische Statuswerte

```text
SUCCESS
VALIDATION_FAILED
BUSINESS_FAILED
INTEGRATION_FAILED
FAILURE
```

Diese Werte helfen, Fehler schneller einzuordnen.

## Aktuell umgesetzt

- Strukturierte Logs in allen .NET-Services.
- OpenTelemetry Logging.
- Loki und Grafana per Docker Compose.
- Automatische Grafana-Datasource für Loki.
- Weniger Framework-Noise.
- Kafka-Fehler werden sauber geloggt.
- REST-Operationen loggen Dauer und Status.

## Noch offen

- Eigene Grafana-Dashboards bauen.
- Alerts für Fehlerfälle ergänzen.
- Optional Traces zusätzlich zu Logs ausbauen.
