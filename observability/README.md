# Observability Stack für die .NET-Services

Dieser Ordner enthält die lokale Logging- und Grafana-Umgebung für die .NET-Services.

Gemeint sind:

- `audit-service-dotnet`
- `warehouse-service-dotnet`
- `invoice-service-dotnet`

Die Java-Services werden hier nicht konfiguriert.

## Zweck

Die .NET-Services schreiben strukturierte Logs.

Diese Logs können über OpenTelemetry an Loki gesendet werden. Grafana zeigt die Logs dann an.

Der Ablauf ist:

```text
.NET Service
  -> OpenTelemetry Collector
  -> Loki
  -> Grafana
```

## Ordnerstruktur

```text
observability/
  grafana/
    provisioning/
      datasources/
        loki.yml
  loki/
    loki-config.yml
  otel-collector/
    otel-collector-config.yml
```

## Starten

Nur Observability starten:

```bash
docker compose up -d loki otel-collector grafana
```

Mit den .NET-Services:

```bash
docker compose up -d --build audit-service warehouse-service invoice-service loki otel-collector grafana
```

Grafana läuft unter:

```text
http://localhost:3000
```

Die Zugangsdaten stehen in der `.env`.

## Prüfen

Container anzeigen:

```bash
docker compose ps
```

Loki prüfen:

```bash
curl http://localhost:3100/ready
```

OpenTelemetry Collector prüfen:

```bash
curl http://localhost:13133
```

Grafana prüfen:

```bash
curl http://localhost:3000/api/health
```

## Grafana benutzen

In Grafana:

```text
Explore -> Loki
```

Nützliche Felder zum Suchen:

- `service`
- `correlationId`
- `operation`
- `eventType`
- `status`
- `errorCode`

Beispiel:

```text
correlationId="91000000-0000-4000-8000-000000000001"
```

Damit kann man einen Bestellablauf über mehrere Services verfolgen.

## Aktueller Stand

Aktuell wurde bereits umgesetzt:

- Loki läuft lokal über Docker Compose.
- Grafana läuft lokal über Docker Compose.
- OpenTelemetry Collector ist vorbereitet.
- Grafana bekommt Loki automatisch als Datasource.
- Die .NET-Services können Logs an den Collector senden.

Noch offen:

- Eigene Dashboards bauen.
- Alerts für Fehlerfälle ergänzen.
- Optional Traces zusätzlich zu Logs stärker nutzen.
