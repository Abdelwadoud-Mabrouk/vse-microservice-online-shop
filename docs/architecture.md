# Architekturdokumentation - Order Processing System

Dieses Dokument beschreibt die Architektur des Microservice-basierten Bestellsystems gemäß den Anforderungen. Das System implementiert einen hybriden Kommunikationsansatz (REST & Kafka) und ein Event-Sourcing-Light System zur Auditierung.

---

## 1. Systemkontext-Diagramm
Das Systemkontext-Diagramm zeigt die Abgrenzung des Systems zu externen Akteuren.

[Systemkontext-Diagramm](./images/system-context.png)

*   **Kunde:** Initiiert den Bestellprozess über den Shop-Service.
*   **Payment-Provider-Stubs:** Externe Schnittstellen (Simulatoren) für die Zahlungsabwicklung (z. B. Klarna, PayPal).
*   **System-Grenze:** Das "Order Processing System" kapselt alle internen Validierungs-, Lager- und Abrechnungsprozesse.

---

## 2. Komponentendiagramm
Übersicht der internen Services, Schnittstellen und Kommunikationswege.

[Komponentendiagramm](./images/component-diagram.png)

*   **Synchron (REST):** Shop-Service → Warehouse-Service (Reservierung) und Shop-Service → Billing-Service (Zahlungsinitiierung).
*   **Asynchron (Kafka):** Billing-Service → Invoice-Service (Rechnungserstellung) und Warehouse-Service (finales Ausbuchen).
*   **Auditierung:** Alle Services senden Status-Events an den zentralen Kafka-Bus, welche vom **Audit-Service** konsumiert und persistiert werden.

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
Kompensationsablauf bei einer fehlgeschlagenen Zahlung.

[Sequenzdiagramm Fehlerszenario](./images/error-scenario.png)

*   **Szenario:** Der Zahlungsanbieter verweigert die Autorisierung (`PAYMENT_FAILED`).
*   **Kompensation:**
    *   Der Billing-Service meldet den Fehler an den Shop-Service zurück.
    *   Der Shop-Service triggert einen Kompensations-Call (`releaseReservation`) am Warehouse-Service.
    *   Der Audit-Service protokolliert den `statusCode: FAILURE` und den finalen Status `COMPENSATED`.

---

## 5. Architectural Decision Records (ADRs)

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

## 6. Erweiterbarkeitsanalyse: Neuer Zahlungsanbieter

Um einen vierten Zahlungsanbieter (z. B. "CryptoPay") zu integrieren, sind folgende Schritte nötig:

### Geänderte Dateien/Komponenten:
*   **Billing-Service (Payment-Fassade):** Implementierung einer neuen Klasse `CryptoPayAdapter`, die das interne `PaymentProvider`-Interface bedient.
*   **Configuration:** Hinzufügen der neuen Provider-Credentials (API-Key, Endpoint) in der `application.properties` oder `config.json`.
*   **Factory/Registry:** Registrierung des neuen Providers in der Fassaden-Logik, damit dieser über das `method`-Feld im Request angesteuert werden kann.

### Unveränderte Dateien/Komponenten:
*   **Shop-Service:** Die Schnittstelle zum Billing-Service bleibt identisch (`POST /payments`).
*   **Invoice- & Warehouse-Service:** Diese erhalten weiterhin nur das standardisierte `PAYMENT_SUCCESS`-Event von Kafka, unabhängig vom gewählten Anbieter.
*   **Audit-Service:** Das Event-Format für Snapshots bleibt konsistent.