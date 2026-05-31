# VSE Microservice Online Shop

Dieses Repository enthält eine Online-Shop-Anwendung als Microservice-Projekt.

Das Ziel ist nicht nur, einzelne Endpunkte zu bauen. Das Ziel ist auch, eine klare Architektur zu zeigen: Services haben getrennte Aufgaben, kommunizieren sauber miteinander und schreiben wichtige Schritte in ein zentrales Audit-System.

## Services im Projekt

Im Projekt gibt es mehrere Services.

Java-Teil:

- `shop-service-java`: nimmt Bestellungen an und steuert den Bestellablauf.
- `billing-service-java`: kümmert sich um Zahlung und Payment-Fassade.

.NET-Teil:

- `warehouse-service-dotnet`: verwaltet Produkte, Bestand und Reservierungen.
- `invoice-service-dotnet`: erstellt Rechnungen als PDF.
- `audit-service-dotnet`: speichert alle wichtigen Ereignisse dauerhaft.

Diese README beschreibt vor allem den .NET-Teil. Die Java-Services werden hier nur grob erwähnt, damit der Gesamtzusammenhang klar ist.

## Wie die Services zusammenarbeiten

Es gibt zwei Arten der Kommunikation.

### REST

REST wird verwendet, wenn ein Service direkt eine Antwort braucht.

Beispiel:

- Der Shop ruft den Warehouse-Service auf.
- Der Warehouse-Service prüft den Bestand.
- Der Shop bekommt direkt zurück, ob die Reservierung funktioniert hat.

Darum bleibt der Warehouse-Service bewusst ein synchroner REST-Service.

### Kafka

Kafka wird verwendet, wenn ein Schritt asynchron passieren kann.

Beispiel:

- Billing meldet: Zahlung war erfolgreich.
- Der Invoice-Service bekommt dieses Event über Kafka.
- Danach erstellt der Invoice-Service die Rechnung.

Auch der Audit-Service kann Events über Kafka empfangen und speichern.

Der Vorteil dieser Lösung ist: Services sind weniger stark gekoppelt. Ein Service muss nicht direkt auf jeden anderen Service warten.

## Zweck des Audit-Service

Der Audit-Service ist der zentrale Ort für wichtige Systemereignisse.

Hier werden z.B. gespeichert:

- Bestellung wurde erstellt
- Lager wurde reserviert
- Zahlung war erfolgreich
- Rechnung wurde erstellt
- Fehler oder Rollback wurde durchgeführt

Alle Events besitzen eine `correlationId`. Damit kann man später alle Schritte einer Bestellung zusammen anzeigen.

Beispiel:

```text
ORDER_CREATED
WAREHOUSE_RESERVED
PAYMENT_SUCCEEDED
INVOICE_CREATED
ORDER_COMPLETED
```

So kann man leicht nachvollziehen, was bei einer Bestellung passiert ist.

## Logging und Grafana

Die .NET-Services schreiben strukturierte Logs.

Wichtige Felder sind:

- `service`
- `correlationId`
- `operation`
- `eventType`
- `status`
- `durationMs`
- `errorCode`

Diese Logs können über OpenTelemetry an Loki gesendet und in Grafana angezeigt werden.

Der praktische Nutzen ist einfach: Wenn etwas kaputtgeht, kann man in Grafana nach einer `correlationId` suchen und sieht den Ablauf über mehrere Services hinweg.

## Verwendete Technologien

Im .NET-Teil werden verwendet:

- .NET 10
- ASP.NET Core Minimal APIs
- Entity Framework Core
- PostgreSQL
- Kafka
- OpenTelemetry
- Loki
- Grafana
- Docker Compose
- xUnit für Tests

## Projekt starten

Vorher eine `.env` Datei anlegen. Als Vorlage kann `.env.example` verwendet werden.

Dann:

```bash
docker compose up -d --build
```

Wichtige URLs:

- Audit Service: `http://localhost:5081`
- Audit Admin UI: `http://localhost:5081/admin`
- Warehouse Service: `http://localhost:5082`
- Invoice Service: `http://localhost:5083`
- Grafana: `http://localhost:3000`
- Loki: `http://localhost:3100`

Swagger ist im Development-Modus aktiv:

- `http://localhost:5081/swagger`
- `http://localhost:5082/swagger`
- `http://localhost:5083/swagger`

Ein einfacher Ablauf zum selbst Testen:

- [Best Case und Worst Case testen](docs/best-worst-case-testablauf.md)

## Tests starten

Mit installiertem .NET 10 SDK:

```bash
/usr/local/share/dotnet/dotnet test OnlineShop.DotNetServices.sln
```

Oder, wenn `dotnet` korrekt auf .NET 10 zeigt:

```bash
dotnet test OnlineShop.DotNetServices.sln
```

## Ordnerstruktur

Wichtige Ordner:

```text
audit-service-dotnet/
invoice-service-dotnet/
warehouse-service-dotnet/
docs/
observability/
docker-compose.yml
OnlineShop.DotNetServices.sln
```

Die .NET-Services sind ähnlich aufgebaut:

```text
Api/              HTTP-Endpunkte, Requests, Middleware
Application/      Use Cases und Interfaces
Domain/           fachliche Modelle und Regeln
Infrastructure/   Datenbank, Kafka, PDF, HTTP-Clients, Logging-Anbindung
Program.cs        Startpunkt und Dependency Injection
```

## Aktueller Stand

Aktuell wurde bereits umgesetzt:

- Warehouse-Service mit Produktbestand, Reservieren, Commit und Cancel.
- Persistente Warehouse-Datenbank mit PostgreSQL.
- Audit-Service mit PostgreSQL und unveränderlichen Audit-Snapshots.
- Invoice-Service mit PDF-Erzeugung und dauerhafter Ablage der Rechnungen.
- Kafka-Consumer für Invoice und Audit.
- Strukturierte Logs für Debugging und Grafana.
- Lokaler Observability-Stack mit Grafana, Loki und OpenTelemetry Collector.
- Admin UI im Audit-Service für Übersicht, Service-Status und Warehouse-Bestand.
- Unit Tests für zentrale Use Cases und Kafka-Deserializer.

Noch offen oder später verbesserbar:

- Java-Services vollständig mit den .NET-Contracts abstimmen.
- Gemeinsame Event-Namen final festlegen.
- Kafka Topics in produktionsnahen Umgebungen explizit anlegen.
- Admin UI weiter absichern und rollenbasiert machen.
- Grafana Dashboards gezielt für Bestellabläufe bauen.
- Weitere Integrationstests über mehrere Services ergänzen.

## Weitere Dokumentation

- [.NET Überblick](docs/dotnet-overview.md)
- [Kafka-Integration im .NET-Teil](docs/kafka-integration-dotnet.md)
- [Audit Flow](docs/audit-flow.md)
- [Logging und Observability](docs/logging-observability.md)
- [Clean Architecture Refactoring](docs/dotnet-clean-architecture-refactor.md)
