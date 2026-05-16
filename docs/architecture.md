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

| Publisher | Event | Consumer | Reaktion |
|-----------|-------|----------|----------|
| Shop-Service | `OrderCreated` | Audit-Service | persistiert Event |
| Shop-Service | `OrderCompleted` | Audit-Service | persistiert Event |
| Shop-Service | `OrderCancelled` | Audit-Service | persistiert Event |

> Optional: Notification-Service subscribed `OrderCancelled` und informiert den Kunden per E-Mail.

---

### Topic `warehouse`

| Publisher | Event | Consumer | Reaktion |
|-----------|-------|----------|----------|
| Warehouse-Service | `StockReserved` | Audit-Service | persistiert Event *(nach REST-Aufruf von Shop)* |
| Warehouse-Service | `StockCommitted` | Audit-Service | persistiert Event *(finale Ausbuchung nach `PaymentSucceeded`)* |
| Warehouse-Service | `StockReservationFailed` | Shop-Service | bricht Bestellung ab → setzt `OrderCancelled` |
| Warehouse-Service | `StockReservationFailed` | Audit-Service | persistiert Event |

---

### Topic `payments`

| Publisher | Event | Consumer | Reaktion |
|-----------|-------|----------|----------|
| Billing-Service | `PaymentInitiated` | Audit-Service | persistiert Event |
| Billing-Service | `PaymentSucceeded` | Invoice-Service | erstellt PDF-Rechnung → publiziert `InvoiceCreated` |
| Billing-Service | `PaymentSucceeded` | Warehouse-Service | bucht Lager endgültig aus → publiziert `StockCommitted` |
| Billing-Service | `PaymentSucceeded` | Audit-Service | persistiert Event |
| Billing-Service | `PaymentFailed` | Warehouse-Service | gibt Reservierung frei |
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

---

## 6. Datenmodellierung

### 6.1 Shop-Service Datenmodell

![Shop ERD](./images/shop-erd.png)
- Order
- OrderItem
- Status

### 6.2 Billing-Service Datenmodell
- Payment
- Transaction

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
