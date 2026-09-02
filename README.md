# VSE Microservice Online Shop

## Projektüberblick

Das Projekt bildet einen verteilten Möbel-Online-Shop ab. Kunden wählen
Produkte, bestellen und bezahlen. Die Services reservieren den Bestand,
verarbeiten die Zahlung, erstellen eine Rechnung und dokumentieren den Ablauf.

## Architektur

Der Shop nutzt REST für Entscheidungen, die eine direkte Antwort benötigen,
und Apache Kafka für fachliche Ereignisse. Shop, Billing, Warehouse, Invoice
und Audit besitzen klare Verantwortlichkeiten und eigene Daten. Das
Angular-22-Frontend bildet die gemeinsame Oberfläche.

## Technologie-Stack

- Angular 22, Standalone Components und Signals
- Java und Spring Boot
- .NET 10
- PostgreSQL
- Apache Kafka
- Docker Compose und Nginx
- OpenTelemetry, Loki und Grafana

## Voraussetzungen

- Docker mit Docker Compose
- freie lokale Ports für Frontend, Services und Grafana
- eine `.env`-Datei auf Basis von `.env.example`

Node.js oder ein lokales Java- beziehungsweise .NET-SDK sind für den
Docker-Start nicht erforderlich.

## Start mit Docker Compose

```bash
cp .env.example .env
docker compose up -d --build
```

Status prüfen:

```bash
docker compose ps
```

## Wichtigste URLs

| Komponente | URL |
|---|---|
| Frontend | `http://localhost:3001` |
| Shop-Service | `http://localhost:8090` |
| Billing-Service | `http://localhost:8091` |
| Audit-Service | `http://localhost:5081` |
| Warehouse-Service | `http://localhost:5082` |
| Invoice-Service | `http://localhost:5083` |
| Grafana | `http://localhost:3000` |

## Tests

.NET-Tests:

```bash
dotnet test OnlineShop.DotNetServices.sln
```

Angular-Tests:

```bash
cd frontend-angular
npm test -- --watch=false
```

## Dokumentation

- [Abschlussbericht](docs/report/abschlussbericht.pdf)
- [Architecture.md](docs/architecture.md)
- [Shop-Service](shop-service-java/README.md)
- [Billing-Service](billing-service-java/README.md)
- [Warehouse-Service](warehouse-service-dotnet/README.md)
- [Invoice-Service](invoice-service-dotnet/README.md)
- [Audit-Service](audit-service-dotnet/README.md)
- [Angular-Frontend](frontend-angular/README.md)

## Team

- Abdelwadoud Mabrouk: .NET-Services
- Mohammed Amine Grioui: Java-Services
- Gemeinsam: Event-Verträge, Integration, Tests, Frontend, Docker Compose, End-to-End-Abläufe und Dokumentation
