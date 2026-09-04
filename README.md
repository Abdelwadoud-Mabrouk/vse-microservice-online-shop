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
### Manuelle Tests & Zahlungsanbieter-Konfiguration

Zur Durchführung manueller Ende-zu-Ende-Tests des Checkout-Prozesses stehen Testdaten für Stripe und PayPal zur Verfügung.

#### 1. Stripe (Kreditkarte)
Für Stripe-Testzahlungen können im Frontend folgende **Stripe-Testkarten** verwendet werden:

* **Erfolgreiche Zahlung (\texttt{PAYMENT\_SUCCEEDED}):**
    * **Kartennummer:** `4242 4242 4242 4242`
    * **Ablaufdatum (MM/YY):** Beliebiges zukünftiges Datum (z. B. `12/30`)
    * **CVC:** Beliebige 3-stellige Zahl (z. B. `123`)
* **Fehlgeschlagene Zahlung (\texttt{PAYMENT\_FAILED} / Card Declined):**
    * **Kartennummer:** `4000 0000 0000 9995`
    * **Ablaufdatum (MM/YY):** Beliebiges zukünftiges Datum
    * **CVC:** Beliebige 3-stellige Zahl

>  **Dokumentation:** Weitere Test-Karten und Fehlerfälle finden Sie in der offiziellen [Stripe Testing Documentation](https://stripe.com/docs/testing).

#### 2. PayPal Sandbox
Um Zahlungen über PayPal im Testmodus durchzuführen:

1. Rufen Sie das [PayPal Developer Dashboard](https://developer.paypal.com/) auf.
2. Erstellen Sie unter **Sandbox → Accounts** einen neuen Test-Käufer-Account (*Personal Account*).
3. Nutzen Sie die dort generierte **Sandbox-E-Mail** und das **Passwort** für den Login im PayPal-Checkout-Window.

>  **Sicherheitshinweis:** Aus Sicherheitsgründen werden keine fertigen PayPal-Zugangsdaten im Quellcode oder im Repository bereitgestellt. Jeder Tester muss für Ende-zu-Ende-Tests eigene Sandbox-Accounts über das PayPal Developer Portal generieren.
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
