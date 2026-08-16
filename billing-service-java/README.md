# Billing-Service

## Aufgabe

Der Billing-Service ist verantwortlich für die zentrale Zahlungsabwicklung des Online-Shops.

Er abstrahiert verschiedene Payment-Provider (z. B. PayPal und Stripe) und stellt eine einheitliche API für Zahlungsprozesse bereit.

Der Service übernimmt:

* Initialisierung von Zahlungen
* Verwaltung von Zahlungsstatus
* Verarbeitung von Rückerstattungen (Refunds)
* Kommunikation mit externen Payment-Providern
* Zuordnung von Transaktionen über Correlation-IDs
* Bereitstellung von Zahlungsinformationen für andere Microservices

Der Billing-Service wird vom Shop-Service während des Bestellprozesses aufgerufen und verarbeitet die Kommunikation mit dem jeweiligen Zahlungsanbieter.

---

## Technologie

* Java 21
* Spring Boot 4
* Spring Web
* Spring Data JPA
* PostgreSQL
* OpenAPI 3 (Swagger)
* Docker
* PayPal API
* Stripe API

---

# API

## Allgemeine Billing API

```text
POST   /api/v1/billing/pay
GET    /api/v1/billing/{correlationId}
POST   /api/v1/billing/refund
GET    /api/v1/billing/status/{transactionId}
```
### Provider-spezifische API

```text
POST   /api/v1/billing/paypal/capture/{token}
```

### Endpunkte

| Methode     | Pfad                      | Beschreibung                                                                  |
| :---------- | :------------------------ | :---------------------------------------------------------------------------- |
| **Payment** |                           |                                                                               |
| POST        | `/pay`                    | Startet einen neuen Zahlungsvorgang über den konfigurierten Payment-Provider. |
| GET         | `/{correlationId}`        | Liefert den Zahlungsstatus anhand der Correlation-ID.                         |
| GET         | `/status/{transactionId}` | Liefert den aktuellen Status einer Transaktion.                               |
| **Refund**  |                           |                                                                               |
| POST        | `/refund`                 | Führt eine Rückerstattung einer bestehenden Zahlung durch.                    |
| **PayPal**  |                           |                                                                               |
| POST        | `/paypal/capture/{token}` | Schließt eine PayPal-Zahlung nach Bestätigung durch den Kunden ab.            |

```text
http://localhost:8081/swagger-ui/index.html
```

und

```text
http://localhost:8081/v3/api-docs.yaml
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

## Zahlungsablauf


Der Billing-Service verwendet ein Provider-Interface, um verschiedene Zahlungsanbieter 
(z. B. PayPal und Stripe) einheitlich anzusprechen.

Der konkrete Payment-Provider wird automatisch anhand der verfügbaren Implementierungen ausgewählt.

```text
Shop-Service
      |
      | Payment Request
      |
      v
Billing-Service
      |
      |
PaymentPluginFacade
      |
      v
PaymentProvider Interface
      |
      +----------------+
      |                |
      v                v
 PayPal Provider   Stripe Provider
      |                |
      v                v
 Payment URL     Payment URL
      |
      v
Billing-Service
      |
      | PaymentResponse
      | paymentUrl
      |
      v
Shop-Service
      |
      v
Frontend
      |
      v
Kunde bezahlt beim Provider
```
## Payment-Fassade

Die Payment-Fassade ist die zentrale Abstraktionsschicht des Billing-Service.
Alle Zahlungsoperationen werden ausschließlich über diese Fassade ausgeführt.

Der Billing-Service kennt keine konkreten Payment-Provider und greift nicht direkt
auf Provider-Implementierungen zu.

Die Kommunikation erfolgt über eine einheitliche Schnittstelle (`PaymentStrategy`),
welche von verschiedenen Payment-Adaptern implementiert wird.

---

### Architektur

```text
                    Billing-Service
                           |
                           |
                           v
                 PaymentPluginFacade
                    (Payment-Fassade)
                           |
                           |
                    PaymentStrategy
                     Interface
                           |
          +----------------+----------------+
          |                                 |
          v                                 v
   PaypalStrategy                     StripeStrategy
          |                                 |
          v                                 v
      PayPal API                         Stripe API
```      
### Erweiterbarkeit (Open/Closed Principle) 
Neue Payment-Provider können hinzugefügt werden, ohne bestehende Komponenten
zu verändern.

```text
Neue Provider Erweiterung:

             PaymentStrategy
                    |
        +-----------+-----------+
        |                       |
 StripeStrategy          PaypalStrategy
                               
Neue:
        |
        v
 KlarnaStrategy

Keine Änderung an:
- BillingService
- PaymentPluginFacade
- Controller
```


## Webhooks

Der Billing-Service verarbeitet externe Webhooks von Payment-Providern.

Beispiele:

* PayPal Payment Completed
* Stripe Payment Succeeded
* Stripe Payment Failed


## Kafka

### Veröffentlichte Events

* PAYMENT_SUCCEEDED
* PAYMENT_FAILED
* REFUND_SUCCEEDED


Diese Events informieren andere Microservices über Änderungen des Zahlungsstatus.

## Datenbank

PostgreSQL speichert 

* Payment
* Transaction
* Provider Information
* Payment Status
* Refund Information
* Correlation-ID
* 
## Fehlerbehandlung

Der Service unterscheidet zwischen

* Validierungsfehlern
* unerwarteten Systemfehlern

Die Fehlerbehandlung erfolgt zentral über einen `@RestControllerAdvice`.

## Docker

```bash
docker compose up -d --build billing-service
```

## Tests

```bash
mvn test