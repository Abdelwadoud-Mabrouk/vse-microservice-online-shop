# Shop-Service

## Aufgabe

Der Shop-Service orchestriert den gesamten Bestellprozess des Online-Shops (Bestellungen, Reservierungen im Warehouse-Service, Zahlungen über den Billing-Service und Statusverwaltung).

Zusätzlich beinhaltet er aktuell die Produktverwaltung (Suchen, Filtern, Details). Diese ist als eigenständiges Package (com.vseonlineshop.shopservice.product) mit eigenen Controllern und DTOs strukturiert, um eine spätere Auslagerung in einen separaten Product Microservice zu ermöglichen.(Modular Monolith)

Der Service koordiniert die Kommunikation zwischen den Microservices und sorgt bei Fehlern für Rollbacks sowie Kafka-Events.

## Technologie

* Java 21
* Spring Boot 3
* Spring Web
* Spring Data JPA
* PostgreSQL
* Apache Kafka
* OpenAPI 3 (Swagger)
* Docker

## API

```text
POST   /api/v1/orders
GET    /api/v1/orders/{id}
POST   /api/v1/orders/capture/{token}

GET    /api/v1/products
GET    /api/v1/products/{id}
GET    /api/v1/products/details/{slug}
GET    /api/v1/products/search
GET    /api/v1/products/filter
```

### Endpunkte

| Methode | Pfad | Beschreibung |
| :--- | :--- | :--- |
| **Orders** | | |
| POST | `/api/v1/orders` | Erstellt eine neue Bestellung, reserviert den Bestand und startet den Zahlungsvorgang. |
| GET | `/api/v1/orders/{id}` | Liefert die Details einer Bestellung. |
| POST | `/api/v1/orders/capture/{token}` | Schließt eine PayPal-Zahlung ab. Dieser Endpunkt wird nur für PayPal verwendet. |
| **Products** | | |
| GET | `/api/v1/products` | Liefert alle aktiven Produkte (paginiert). |
| GET | `/api/v1/products/{id}` | Liefert Produktdetails anhand der ID. |
| GET | `/api/v1/products/details/{slug}` | Liefert Produktdetails anhand des Slugs. |
| GET | `/api/v1/products/search` | Volltextsuche nach Produkten. |
| GET | `/api/v1/products/filter` | Erweiterter Filter (Kategorie, Farbe, Material, Preisspanne). |

Die vollständige OpenAPI-Beschreibung der laufenden Anwendung ist verfügbar unter

```text
http://localhost:8090/swagger-ui/index.html
```

und

```text
http://localhost:8090/v3/api-docs.yaml
```

Eine statische Version der OpenAPI-Spezifikationen aller Microservices befindet sich im Repository unter

```text
docs/openapi/
```

Dort sind unter anderem enthalten:

* `shop-service.yaml`
* `billing-service.yaml`
* `warehouse-service.yaml`
* `audit-service.yaml`
* `ivoice-service.yaml`

## Bestellablauf

```text
Client
   │
   ▼
POST /api/v1/orders
   │
   ▼
Order anlegen
   │
   ▼
Warehouse reservieren
   │
   ▼
Zahlung initialisieren
   │
   ▼
Status = AWAITING_PAYMENT
   │
   ▼
Payment bestätigt
   │
   ▼
Order = COMPLETED
```

Die Art der Zahlungsbestätigung hängt vom verwendeten Zahlungsanbieter ab:

* **PayPal:** Abschluss über `POST /api/v1/orders/capture/{token}`.
* **Stripe:** Abschluss über den Payment-Workflow (z. B. Webhook oder Payment Intent). Ein separater Capture-Endpunkt ist nicht erforderlich.

Bei Fehlern werden Reservierungen automatisch freigegeben und entsprechende Kafka-Events veröffentlicht.

## Kafka

### Veröffentlichte Events

* `ORDER_CREATED`
* `ORDER_CANCELLED`
* `RESERVATION_ROLLBACK_REQUESTED`
* `WAREHOUSE_DISPATCH_FAILED`
* `SYSTEM_ALERT`

### Konsumierte Events

Der Shop-Service konsumiert Ereignisse anderer Microservices, um den Bestellstatus zu aktualisieren, beispielsweise erfolgreiche oder fehlgeschlagene Zahlungen sowie Ereignisse des Warehouse-Services.

## Datenbank

PostgreSQL speichert unter anderem

* Orders
* Order Items
* Kundeninformationen
* Lieferadressen
* Zahlungsinformationen
* Reservierungsreferenzen

Jede Bestellung besitzt zusätzlich eine **Correlation-ID**, mit der der komplette Ablauf über mehrere Microservices hinweg nachvollzogen werden kann.

## Fehlerbehandlung

Der Service unterscheidet zwischen

* Validierungsfehlern
* Business Exceptions (z. B. kein Bestand verfügbar)
* ServiceUnavailable Exceptions (Warehouse- oder Billing-Service nicht erreichbar)
* unerwarteten Systemfehlern

Die Fehlerbehandlung erfolgt zentral über einen `@RestControllerAdvice`.

## Docker

```bash
docker compose up -d --build shop-service
```

## Tests

```bash
mvn test
```
