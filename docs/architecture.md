# Architekturdokumentation - Order Processing System

Dieses Dokument beschreibt die Architektur des Microservice-basierten Bestellsystems gemäß den Anforderungen. Das System implementiert einen hybriden Kommunikationsansatz (REST & Kafka) und ein Event-Sourcing-Light System zur Auditierung.

> Die aktuellen Mermaid-Diagramme für die Gesamtarchitektur befinden sich in [system-diagrams.md](./system-diagrams.md).

> Hinweis: Die aktuell implementierten .NET-Services (Warehouse, Invoice, Audit) sind zusätzlich mit Mermaid-Diagrammen in [dotnet-services-diagrams.md](./dotnet-services-diagrams.md) dokumentiert.

---

## 1. Systemkontext-Diagramm

Das Systemkontext-Diagramm beschreibt die Abgrenzung des „Order Processing Systems“ zu externen Akteuren und externen Systemen. Es zeigt, welche Benutzer und Fremdsysteme mit der Microservice-Landschaft interagieren.


[Systemkontext-Diagramm](./images/system-context.png)

![img.png](images/img.png)

*   **Kunde:** Initiiert den Bestellprozess über den Shop-Service.
*   **Payment-Provider-Stubs:** Externe Schnittstellen (Simulatoren) für die Zahlungsabwicklung (z. B. Klarna, PayPal).
*   **System-Grenze:** Das "Order Processing System" kapselt alle internen Validierungs-, Lager- und Abrechnungsprozesse.

---

## 2. Komponentendiagramm
Die Architektur basiert auf einem hybriden Kommunikationsansatz aus synchronen REST-Aufrufen und asynchroner Event-Kommunikation über Kafka.

![img.png](./images/system-context.png)

Das System besteht aus mehreren unabhängigen Microservices:

* Shop-Service
* Warehouse-Service
* Billing-Service
* Payment-Fassade
* Invoice-Service
* Audit-Service

Die Services kommunizieren über einen hybriden Ansatz aus synchroner REST-Kommunikation und asynchroner Event-Kommunikation über Kafka.

* **Synchron:**
    * Shop-Service ↔ Warehouse-Service
    * Shop-Service ↔ Billing-Service

* **Asynchron (Kafka Events):**
    * Billing-Service → Invoice-Service
    * Billing-Service → Warehouse-Service
    * Alle Services → Audit-Service

Jeder Service besitzt eine eigene Datenbank („Database per Service“), um geringe Kopplung, unabhängige Skalierung und klare Verantwortlichkeiten zu gewährleisten.
Übersicht der internen Services, Schnittstellen und Kommunikationswege.

Zusätzlich umfasst das System eine Monitoring- und Logging-Infrastruktur:

![img_2.png](images/monitoring.png)


* **Prometheus**
    * sammelt Metriken aller Services

* **Loki**
    * zentralisiert Logs aller Services

* **Grafana**
    * visualisiert Metriken und Logs für Monitoring und Fehleranalyse durch Administratoren
---

## 3. Sequenzdiagramm - Happy Path (UML)
Der erfolgreiche Ablauf einer Bestellung ohne Fehlerzustände.

![Sequenzdiagramm Happy Path](./images/happy-path.png)

**Kernschritte:**
1. **Order Initiation:** Shop-Service generiert eine `correlationId` und publiziert `ORDER_PLACED`.
2. **Synchronous Reservation:** Warehouse-Service reserviert Artikel via REST.
3. **Synchronous Payment:** Billing-Service delegiert via Payment-Fassade an den Provider.
4. **Asynchronous Processing:** Nach `PAYMENT_SUCCESS` erstellt der Invoice-Service die PDF und das Warehouse bucht den Bestand endgültig aus.
5. **Completion:** Shop-Service setzt den Status auf `COMPLETED`.

---

## 4. Sequenzdiagramm - Fehlerszenario (UML)

[Sequenzdiagramm Fehlerszenario](./images/error-scenario.png)

---

# 5. Event-Konzept

## Architektur-Grundlage

- **Kafka** ist der zentrale Message Bus für alle asynchronen Prozesse
- Zwei Verbindungen bleiben **synchron per REST**, weil der Shop-Service die Antwort abwarten muss, bevor der Bestellstatus gesetzt werden kann

---

## Synchron REST

| Von | Nach | Grund |
|-----|------|-------|
| Shop-Service | Warehouse-Service | Lagerprüfung und Reservierung müssen bestätigt sein |
| Shop-Service | Billing-Service | Zahlungsbestätigung muss vorliegen bevor `COMPLETED` gesetzt wird |

---

## Asynchron Kafka — Topics & Events

### Topic `orders`

| Publisher | Event | Consumer | Reaktion                                                                     |
|-----------|-------|----------|------------------------------------------------------------------------------|
| Shop-Service | `OrderCreated` | Audit-Service | persistiert Event                                                            |
| Shop-Service | `OrderCompleted` | Audit-Service | persistiert Event                                                            |
| Shop-Service | `OrderCancelled` | Audit-Service | persistiert Event                                                            |
| Shop-Service | `OrderCancelled` | Warehouse-Service | Gibt die blockierten Items für diese reservartionId sofort wieder frei!      ||
| Shop-Service | `ReservationRollbackRequested` | Warehouse-Service | Nimmt die reservationId, gibt die Items frei und wirft danach StockReleased. |

> Optional: Notification-Service subscribed `OrderCancelled` und informiert den Kunden per E-Mail.

---

### Topic `warehouse`

| Publisher | Event | Consumer | Reaktion                                                          |
|-----------|-------|----------|-------------------------------------------------------------------|
| Warehouse-Service | `StockReserved` | Audit-Service | persistiert Event *(nach REST-Aufruf von Shop)*                   |
| Warehouse-Service | `StockCommitted` | Audit-Service | persistiert Event *(finale Ausbuchung nach `PaymentSucceeded`)*   |
| Warehouse-Service | `StockReleased` | Audit-Service | Persistiert Event *(Blockierung nach OrderCancelled aufgehoben)*  |
| Warehouse-Service | `StockReservationFailed` | Audit-Service | persistiert Event(Lager war leer / Fehler).                                                 |

---

### Topic `payments`

| Publisher | Event | Consumer | Reaktion |
|-----------|-------|----------|----------|
| Billing-Service | `PaymentInitiated` | Audit-Service | persistiert Event |
| Billing-Service | `PaymentSucceeded` | Invoice-Service | erstellt PDF-Rechnung → publiziert `InvoiceCreated` |
| Billing-Service | `PaymentSucceeded` | Warehouse-Service | bucht Lager endgültig aus → publiziert `StockCommitted` |
| Billing-Service | `PaymentSucceeded` | Audit-Service | persistiert Event |
| Billing-Service | `PaymentFailed` | Audit-Service | persistiert Event |

---

### Topic `invoices`

| Publisher | Event | Consumer | Reaktion |
|-----------|-------|----------|----------|
| Invoice-Service | `InvoiceCreated` | Audit-Service | persistiert Event |

> Optional: Notification-Service subscribed `InvoiceCreated` und verschickt die PDF-Rechnung per E-Mail an den Kunden.

---

## Audit-Service — gilt für alle Topics

- Subscribed **alle vier Topics**: `orders`, `warehouse`, `payments`, `invoices`
- Schreibt jeden eingehenden Event in seine eigene **PostgreSQL-Datenbank**
- Prinzip: **nur Insert** — niemals Update oder Delete (append-only / immutable log)
- Zweck: lückenlose, unveränderliche Protokollierung aller Systemaktivitäten für Compliance und Nachvollziehbarkeit

---

## Übersicht — alle Events auf einen Blick

| Event | Topic | Publisher | Happy Path |
|-------|-------|-----------|------------|
| `OrderCreated` | `orders` | Shop-Service | ✅ |
| `StockReserved` | `warehouse` | Warehouse-Service | ✅ |
| `PaymentInitiated` | `payments` | Billing-Service | ✅ |
| `PaymentSucceeded` | `payments` | Billing-Service | ✅ |
| `InvoiceCreated` | `invoices` | Invoice-Service | ✅ |
| `StockCommitted` | `warehouse` | Warehouse-Service | ✅ |
| `OrderCompleted` | `orders` | Shop-Service | ✅ |
| `StockReservationFailed` | `warehouse` | Warehouse-Service | ❌ Fehlerfall |
| `PaymentFailed` | `payments` | Billing-Service | ❌ Fehlerfall |
| `OrderCancelled` | `orders` | Shop-Service | ❌ Fehlerfall |
| `ReservationRollbackRequested` | `orders` | Shop-Service | ❌ Fehlerfall |



## Problem: spezifische Event-DTOs pro Ereignis

Ein naiver Ansatz würde für jedes Ereignis eine eigene Klasse erstellen:

```
OrderCreatedEvent.java
OrderCompletedEvent.java
OrderCancelledEvent.java
ReservationRollbackEvent.java
PaymentSucceededEvent.java
...
```

Jede Klasse trägt dabei dieselben Metadaten – `eventId`, `source`, `occurredAt`,
`correlationId` – redundant wiederholt. Der AuditService müsste jeden dieser Typen
kennen und separat behandeln. Kommt ein neuer Event-Typ hinzu, muss der AuditService
angepasst werden, obwohl er inhaltlich nichts Neues tut.

---

## Lösung: Envelope Pattern mit `DomainEvent<T>`

```
┌────────────────────────────────────────────┐
│              DomainEvent<T>                │  ← einheitlicher Wrapper (Envelope)
│                                            │
│  eventId        UUID       (Deduplizierung)│
│  eventType      String     ("ORDER_CREATED")│
│  source         String     ("shop-service") │
│  correlationId  UUID       (Flow-Tracking)  │
│  occurredAt     LocalDateTime               │
│  payload        T          ← fachlicher    │
│                               Inhalt        │
└────────────────────────────────────────────┘
```

Der Envelope trennt zwei Verantwortlichkeiten sauber:

| Teil | Inhalt | Ändert sich |
|---|---|---|
| Envelope | technische Metadaten | selten |
| Payload `T` | fachlicher Inhalt pro Event | häufig, pro Service |

### Vorteile

**1 – AuditService ist vollständig entkoppelt**

Der AuditService konsumiert Events aus allen Topics (`orders`, `warehouse`,
`payments`, `invoices`) und persistiert alles. Er muss `payload` nie
deserialisieren – er speichert es als rohen JSON-String (JSONB):

```java
DomainEvent<JsonNode> envelope = objectMapper.readValue(raw, new TypeReference<>(){});

AuditEvent.builder()
    .eventId(envelope.eventId())
    .eventType(envelope.eventType())     // "ORDER_CANCELLED"
    .source(envelope.source())           // "shop-service"
    .correlationId(envelope.correlationId())
    .payload(envelope.payload().toString()) // roher JSON-Blob → JSONB
    .build();
```

Ein neuer Event-Typ aus einem neuen Service wird **automatisch gespeichert**,
ohne eine einzige Code-Änderung im AuditService.

**2 – Idempotenz über `eventId`**

Kafka garantiert *at-least-once*. Dasselbe Event kann doppelt ankommen.
Jeder Consumer kann Duplikate anhand der `eventId` erkennen und überspringen:

```java
if (processedEventRepository.existsByEventId(envelope.eventId())) {
    return; // Duplikat – bereits verarbeitet
}
```

**3 – Korrelation über den gesamten Flow**

Alle Events einer einzigen Order tragen dieselbe `correlationId`.
Im AuditService lässt sich damit der komplette Lebenszyklus einer Order
in chronologischer Reihenfolge rekonstruieren:

```
correlationId: abc-123
├── ORDER_CREATED             (shop-service)
├── STOCK_RESERVED            (warehouse-service)
├── PAYMENT_INITIATED         (shop-service)
├── PAYMENT_SUCCEEDED         (payment-service)
└── ORDER_COMPLETED           (shop-service)
```

**4 – Andere Services deserialisieren nur was sie brauchen**

WarehouseService und InvoiceService empfangen ebenfalls `DomainEvent<JsonNode>`,
prüfen `eventType` und deserialisieren `payload` nur bei relevanten Events
in ihre eigene lokale Payload-Klasse:

```java
switch (envelope.eventType()) {
    case "RESERVATION_ROLLBACK_REQUESTED" -> {
        ReservationRollbackPayload p =
            objectMapper.treeToValue(envelope.payload(), ReservationRollbackPayload.class);
        reservationService.rollback(p.reservationId());
    }
    default -> { /* ignorieren */ }
}
```

---

## Shared-Kernel Modul

`DomainEvent<T>` ist ein **technischer Vertrag**, der von allen Services
gleich interpretiert werden muss. Deswegen liegt er in einem minimalen
`shared-kernel`-Modul.

```
vseonlineshop/
├── shared-kernel/                ← nur DomainEvent<T>, keine fachlichen Klassen
│   └── DomainEvent.java
│
├── shop-service/                 ← dependency: shared-kernel
│   └── event/
│       ├── OrderCreatedPayload.java     ← fachlich, bleibt lokal
│       └── OrderCompletedPayload.java
│
├── warehouse-service/            ← dependency: shared-kernel
│   └── event/
│       └── ReservationRollbackPayload.java  ← eigene lokale Kopie
│
└── audit-service/                ← dependency: shared-kernel
    └── (keine Payload-Klassen)   ← JsonNode reicht
```

### Warum Payload-DTOs NICHT im shared-kernel?

Payload-DTOs sind **fachliche Verträge** die sich häufig ändern. Würden sie
geteilt, entsteht versteckte Kopplung: eine Änderung in `OrderCreatedPayload`
im Shop zwingt alle Services zum gleichzeitigen Update – der Kernvorteil
von Microservices (unabhängige Deployments) geht verloren.

Jeder Consumer definiert seine eigene lokale Payload-Klasse mit genau den
Feldern, die er braucht. Felder die er nicht kennt, ignoriert Jackson
automatisch (`FAIL_ON_UNKNOWN_PROPERTIES = false`).

| | `DomainEvent<T>` | Payload DTOs |
|---|---|---|
| Charakter | technisch | fachlich |
| Ändert sich | selten | häufig |
| Wo | `shared-kernel` | lokal pro Service |
| Wer braucht es | alle Services | nur der jeweilige Consumer |
---

## 6. Service Design

### 6.1 Shop-Service Design

### ER-Diagramm
![Shop](./images/shop-erd.png)

### Entscheidung: Verwendung von UUIDs als Primärschlüssel

**Begründung:**

* **Schutz vor ID Enumeration Attacks**
  * Fortlaufende IDs (`/orders/1001`) sind leicht erratbar.
  * UUIDs sind zufällig und nicht vorhersagbar.
  * Erhöht die Sicherheit gegen unbefugten Zugriff auf fremde Ressourcen.

* **Dezentrale ID-Generierung**
  * IDs können direkt im Service generiert werden (`UUID.randomUUID()`).
  * Keine Abhängigkeit von der Datenbank zur ID-Erzeugung.

* **Asynchrone Entkopplung**
  * Die ID steht bereits vor der Persistierung für Kafka-Events und REST-Aufrufe bereit.
  * Unterstützt event-getriebene Microservice-Kommunikation ohne blockierende Datenbankoperationen.

* **Schutz von Geschäftsgeheimnissen**
  * UUIDs verhindern Rückschlüsse auf Bestellvolumen oder Unternehmenswachstum durch fortlaufende IDs.

* **Globale Eindeutigkeit**
  * UUIDs sind weltweit eindeutig.
  * Mehrere Services können unabhängig Entitäten erzeugen, ohne ID-Konflikte zu riskieren.

--- 

### API-Design

### Synchroner Flow mit parallelem Kafka

```
Client ──POST /orders──► Shop-Service
   { productId, quantity }     │
                               │──── Kafka: OrderCreated ──► AuditService
                               │                             (fire & forget)
                    ┌──────────▼──────────┐
                    │ 1. Warehouse-Service │ (REST sync)
                    │  POST /reservations  │
                    │  ← reservationId     │
                    │  ← productName       │──── Kafka: StockReserved ──► AuditService
                    │  ← unitPrice         │                              (fire & forget)
                    └──────────┬───────────┘
                               │ Shop berechnet totalAmount
                    ┌──────────▼──────────┐
                    │ 2. Billing-Service   │ (REST sync)
                    │  POST /payments      │──── Kafka: PaymentInitiated ──► AuditService
                    │  { totalAmount }     │                                 (fire & forget)
                    │                      │
                    │  Payment Provider    │
                    │  returns SUCCESS     │
                    │  ← 200 CONFIRMED     │──── Kafka: PaymentSucceeded ──► InvoiceService
                    └──────────┬───────────┘                             ──► AuditService
                               │                                         ──► Warehouse
                    ┌──────────▼──────────┐                                  (alle async)
                    │ 3. DB Update         │
                    │  status = COMPLETED  │──── Kafka: OrderCompleted ──► AuditService
                    └──────────┬───────────┘                               (fire & forget)
                               │
Client ◄── 201 COMPLETED ──────┘
```

> **Kafka = fire-and-forget:** Jeder Service publiziert seinen Event
> sofort und wartet nicht auf Consumer. Der Client wartet
> ausschliesslich auf die synchrone REST-Kette.

---

### POST /orders

### Request vom Client

> Client schickt **keine Preise** — Preise kommen von Kategorie.
> Verhindert Preismanipulation durch den Client.

```http
POST /orders
Content-Type: application/json
```

```json
{
  "userId": "uuid-optional",
  "items": [
    { "productId": "A1", "quantity": 2 },
    { "productId": "B3", "quantity": 1 }
  ],
  "currency": "EUR"
}
```

**Intern generiert (nicht vom Client):**
- `id` / `correlationId` → eine UUID, vor DB-Insert generiert
- `totalAmount` → berechnet aus Warehouse-Antwort: `sum(quantity × unitPrice)`
- `status` → startet intern als `PENDING`
- `createdAt`, `updatedAt` → Timestamps

---

### Schritt 1 — Shop → Warehouse (intern)

**Shop schickt an Warehouse:**
```json
POST /reservations

{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "order_id": "550e8400-e29b-41d4-a716-446655440001",
  "items": [
    { "productId": "A1", "quantity": 2 },
    { "productId": "B3", "quantity": 1 }
  ]
}
```

**Warehouse antwortet Shop mit `201`:**
```json
{
  "reservationId": "550e8400-e29b-41d4-a716-446655440000",
  "order_id": "550e8400-e29b-41d4-a716-446655440001",
  "items": [
    {
      "productId": "A1",
      "productName": "Nike Air Max",
      "quantity": 2,
      "unitPrice": 89.99
    },
    {
      "productId": "B3",
      "productName": "Adidas Hose",
      "quantity": 1,
      "unitPrice": 49.99
    }
  ]
}
```

**Was Shop aus dieser Antwort macht:**

| Feld | Verwendung |
|------|------------|
| `reservationId` | Gespeichert in ORDER — nötig um Reservierung bei `PaymentFailed` freizugeben |
| `productName` | Snapshot → gespeichert in ORDER_ITEM |
| `unitPrice` | Snapshot → gespeichert in ORDER_ITEM + Berechnung totalAmount |
| `totalAmount` | `(2 × 89.99) + (1 × 49.99) = 229.97` → weiter an Billing |

---

### Schritt 2 — Shop → Billing (intern)

```json
POST /payments

{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "order-uuid",
  "totalAmount": 229.97,
  "currency": "EUR"
}
```
**Billing antwortet Shop — Erfolg `200 OK`:**
```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "paymentId": "pay-uuid-9c1f",
  "status": "CONFIRMED",
  "paidAt": "2024-01-15T10:00:02Z"
}
```

**Billing antwortet Shop — Fehler `402`:**
```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FAILED",
  "reason": "INSUFFICIENT_FUNDS"
}
```
---

### Response — Erfolg `201 Created`

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "totalAmount": 229.97,
  "currency": "EUR",
  "createdAt": "2024-01-15T10:00:00Z",
  "items": [
    {
      "productId": "A1",
      "productName": "Nike Air Max",
      "quantity": 2,
      "unitPrice": 89.99
    },
    {
      "productId": "B3",
      "productName": "Adidas Hose",
      "quantity": 1,
      "unitPrice": 49.99
    }
  ]
}
```

### Response — Fehler

#### `400 Bad Request`
```json
{ "status": 400, "error": "BAD_REQUEST", "message": "items must not be empty" }
```

#### `409 Conflict` — Lager nicht verfügbar
```json
{ "status": 409, "error": "STOCK_UNAVAILABLE", "message": "Product A1 is out of stock" }
```

#### `402 Payment Required` — Zahlung fehlgeschlagen
```json
{ "status": 402, "error": "PAYMENT_FAILED", "message": "Payment declined by provider" }
```

#### `503 Service Unavailable`
```json
{ "status": 503, "error": "SERVICE_UNAVAILABLE", "message": "Downstream service temporarily unavailable." }
```

---

## GET /orders/{orderId}

```http
GET /orders/550e8400-e29b-41d4-a716-446655440000
```

### Response `200 OK`
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "totalAmount": 229.97,
  "currency": "EUR",
  "createdAt": "2024-01-15T10:00:00Z",
  "items": [
    { "productId": "A1", "productName": "Nike Air Max", "quantity": 2, "unitPrice": 89.99 },
    { "productId": "B3", "productName": "Adidas Hose",  "quantity": 1, "unitPrice": 49.99 }
  ]
}
```

### Response `404 Not Found`
```json
{ "status": 404, "error": "ORDER_NOT_FOUND", "message": "Order 550e8400 not found" }
```

---

## GET /actuator/health
```json
{ "status": "UP" }
```

---

## Status-Übergänge

```
PENDING ──► PROCESSING ──► COMPLETED
                │
                └──► CANCELLED  (Stock nicht verfügbar / Payment fehlgeschlagen)
```

| Status | Wann | Sichtbar für Client? |
|--------|------|-------------------|
| `PENDING` | Order angelegt | intern            |
| `PROCESSING` | Warehouse OK, Payment läuft |  intern           |
| `COMPLETED` | Alles erfolgreich | in Response       |
| `CANCELLED` | Fehler bei Warehouse oder Payment |  als Error-Response |

### 6.2 Billing-Service Design

### ER-Diagramm
![Payment](./images/payment-erd.png)

**Begründung:**

* **Trennung von Business- und Integrationslogik**
  * `PAYMENT` repräsentiert den fachlichen Zahlungsstatus (Business View)
  * `PAYMENT_TRANSACTION` bildet einzelne Provider-Aufrufe ab (technische Historie)
  * Dadurch klare Separation zwischen Domänenlogik und externer Kommunikation

* **Unterstützung von Retry- und Timeout-Szenarien**
  * Eine Zahlung kann mehrere Provider-Requests enthalten
  * Jeder Versuch wird als eigene Transaction gespeichert
  * Ermöglicht Nachvollziehbarkeit bei Fehlern und Wiederholungen

* **Provider-Unabhängigkeit**
  * System ist nicht an einen Zahlungsanbieter gebunden
  * Unterschiedliche Provider (Stripe, PayPal, Klarna) werden einheitlich modelliert

* **Nachvollziehbarkeit & Auditfähigkeit**
  * `raw_response` speichert vollständige Provider-Antworten
  * Ideal für Debugging, Supportfälle und finanzielle Reconciliation

* **fee_amount**
  * Transaktionsgebühr des Providers
  * Wichtig zur Berechnung des tatsächlichen Netto-Umsatzes

---



### API-Design

### Synchroner Checkout-Flow mit Provider-Delegation


```
Shop-Service ──POST /payments──► Payment-Service
(PaymentRequest DTO)                  │
                                      │──► 1. Persistiert Transaktion (INITIALIZED)
                                      │──► 2. Kafka: PaymentInitiated ──► Audit-Service
                                      │
                           ┌──────────▼──────────┐
                           │ PaymentPluginFacade │ (Echtzeit-Auswahl)
                           └──────────┬──────────┘
                                      │ Dynamisches Routing via Name
                           ┌──────────▼──────────┐
                           │   PayPal / Stripe   │ (Externe Provider-API)
                           │   ← Return SUCCESS  │
                           └──────────┬──────────┘
                                      │
                           ┌──────────▼──────────┐
                           │ 3. DB Update         │
                           │  status = COMPLETED  │──► Kafka: PaymentSucceeded ──► Invoice-Service
                           └──────────┬───────────┘                             ──► Warehouse-Service
                                      │                                         ──► Audit-Service
Shop-Service ◄── 200 CONFIRMED ───────┘

```
---

### POST /payments

### Request vom Shop-Service

POST /payments

Content-Type: application/json
```json
{
  "correlationId": "482e1c4c-74ee-4940-8cfd-5883c4fd6b9e",
  "orderId": "dd1a9ef5-adfa-48cf-a7b8-8a45d82103be",
  "totalAmount": 898.99,
  "currency": "EUR",
  "paymentProvider": "PAYPAL",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```
---

### Response vom Payment-Service

#### Erfolg 200 OK
```json 
{
  "correlationId": "482e1c4c-74ee-4940-8cfd-5883c4fd6b9e",
  "orderId": "dd1a9ef5-adfa-48cf-a7b8-8a45d82103be",
  "paymentId": "74fb29d1-d3f2-469d-ae9e-d838fd9fa51d",
  "status": "CONFIRMED",
  "paidAt": "2026-05-21T19:30:00Z"
}
```
---
#### Fehler 402 Payment Required (Kreditkarte abgelehnt / Zu wenig Guthaben)
```json
{
  "correlationId": "482e1c4c-74ee-4940-8cfd-5883c4fd6b9e",
  "orderId": "dd1a9ef5-adfa-48cf-a7b8-8a45d82103be",
  "status": "FAILED",
  "reason": "INSUFFICIENT_FUNDS"
}
```
--- 
## GET /payments/{transactionId}
```json
{
  "transactionId": "74fb29d1-d3f2-469d-ae9e-d838fd9fa51d",
  "correlationId": "482e1c4c-74ee-4940-8cfd-5883c4fd6b9e",
  "orderId": "dd1a9ef5-adfa-48cf-a7b8-8a45d82103be",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "totalAmount": 898.99,
  "currency": "EUR",
  "paymentProvider": "PAYPAL",
  "status": "COMPLETED",
  "reason": null,
  "createdAt": "2026-05-21T19:29:42Z"
}
```
---

## GET /actuator/health
```json
{ "status": "UP" }
```
--- 
### Status-Übergänge der Transaktion

```
INITIALIZED──►PROCESSING ──► COMPLETED
                │
                └──► FAILED (z.B. Timeout, Limit überschritten, Abbruch durch User)
```
--- 
### Software-Design: Offen für Erweiterungen (Plugin-Muster)

Um das System hochgradig wartbar zu machen und neue Zahlungsanbieter ohne Seiteneffekte hinzuzufügen,
wird das Strategy Pattern (Kombiniert mit einer Fassade) implementiert.

#### 1. Das Interface für die Provider-Strategie

Jeder neue Zahlungsanbieter implementiert dieses Interface:

```
 public interface PaymentStrategy {
    boolean supports(PaymentProvider provider);
    ProviderResponse processPayment(PaymentRequest request);
}
```
---
#### 2. Die konkrete Implementierung (Beispiel PayPal)

```
@Component
public class PaypalPaymentStrategy implements PaymentStrategy {
    @Override
    public boolean supports(PaymentProvider provider) {
        return provider == PaymentProvider.PAYPAL;
    }

    @Override
    public ProviderResponse processPayment(PaymentRequest request) {
        // Logik für den echten PayPal SDK-Aufruf
        return new ProviderResponse(true, null);
    }
}
```
---
##### 3. Die delegierende Fassade (Der Core-Service)

Der eigentliche Service injiziert vollautomatisch alle verfügbaren Strategien in einer Liste. Kommt ein Request rein, 
sucht er sich den passenden Provider dynamisch heraus:

```
@Service
@RequiredArgsConstructor
public class PaymentPluginFacade {

    private final List<PaymentStrategy> strategies;

    public ProviderResponse execute(PaymentRequest request) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(request.paymentProvider()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Provider nicht unterstützt!"))
                .processPayment(request);
    }
}
```

Vorteil für die Weiterentwicklung: Wenn im nächsten Sprint Klarna oder Stripe hinzukommt, schreibt dein Kollege einfach eine neue Klasse, die PaymentStrategy implementiert und versieht sie mit @Component. Der bestehende Zahlungskern muss weder angefasst, 
noch getestet, noch verändert werden (Einhaltung des Open-Closed-Prinzips).
---
# Payment Service — Must-DOs

## 1. Idempotency-Key
- Jeden API-Call mit eindeutiger `order_id` / `payment_id` als Header senden
- Provider erkennt Retry → kein doppeltes Abbuchen
- Verhindert Double-Charging bei Timeout / Netzwerkfehler

## 2. Währung — Minor Units
- Provider rechnet in Cent (kleinste Einheit), nicht in BigDecimal
- `898.99 EUR` → `89899` (× 100)
- Achtung: JPY hat keine Cents, KWD hat 3 Stellen → immer Doku prüfen

## 3. TLS 1.2 / 1.3 erzwingen
- Stripe / PayPal blockieren alte TLS-Versionen hart
- In Spring Boot: `SSLContextBuilder` mit `TLSv1.3` konfigurieren

## 4. Provider-Fehler robust behandeln
- `402` → Karte abgelehnt → Saga-Kompensation auslösen
- `408` → Timeout → Retry mit Idempotency-Key
- `500` → Provider-Fehler → Retry + Alert
- Fehler dürfen System nie inkonsistent hinterlassen

## 5. Audit-Logging — rawResponse persistieren
- Komplette Provider-Antwort in DB speichern (`raw_response`)
- Pflicht für: Debugging, Streitfälle, Finanz-Audit

---


## 7. Architectural Decision Records (ADRs)

### ADR 1: Wahl des Message Brokers (Kafka)
*   **Entscheidung:** Nutzung von Apache Kafka für die inter-Service Kommunikation.
*   **Begründung:** Kafka bietet hohe Persistenz und Skalierbarkeit. Es ermöglicht dem Invoice-Service und Warehouse-Service, Events unabhängig voneinander zu konsumieren, auch wenn ein Service kurzzeitig offline ist (Resilienz).

### ADR 2: Persistenzstrategie für Audit-Snapshots
*   **Entscheidung:** Relationale Datenbank mit Append-Only Strategie im Audit-Service.
*   **Begründung:** Um Unveränderlichkeit zu garantieren, werden Snapshots nur per `INSERT` gespeichert. Die relationale Struktur erlaubt eine effiziente Abfrage über `GET /audit/orders/{correlationId}` sortiert nach Zeitstempeln.

### ADR 3: Kommunikationsmuster (Hybrider Ansatz)
*   **Entscheidung:** Synchrones REST für User-Interaktionen, asynchrones Kafka für Hintergrundprozesse.
*   **Begründung:** Bestandsprüfung und Zahlung erfordern sofortiges Feedback für den Kunden. Rechnungsdruck und Archivierung sind zeitunkritisch und werden zur Lastverteilung asynchron entkoppelt.

---

## 8. Erweiterbarkeitsanalyse

Um einen vierten Zahlungsanbieter (z. B. "CryptoPay") zu integrieren, sind folgende Schritte nötig:

### Geänderte Dateien/Komponenten:
*   **Billing-Service (Payment-Fassade):** Implementierung einer neuen Klasse `CryptoPayAdapter`, die das interne `PaymentProvider`-Interface bedient.
*   **Configuration:** Hinzufügen der neuen Provider-Credentials (API-Key, Endpoint) in der `application.properties` oder `config.json`.
*   **Factory/Registry:** Registrierung des neuen Providers in der Fassaden-Logik, damit dieser über das `method`-Feld im Request angesteuert werden kann.

### Unveränderte Dateien/Komponenten:
*   **Shop-Service:** Die Schnittstelle zum Billing-Service bleibt identisch (`POST /payments`).
*   **Invoice- & Warehouse-Service:** Diese erhalten weiterhin nur das standardisierte `PAYMENT_SUCCESS`-Event von Kafka, unabhängig vom gewählten Anbieter.
*   **Audit-Service:** Das Event-Format für Snapshots bleibt konsistent.
