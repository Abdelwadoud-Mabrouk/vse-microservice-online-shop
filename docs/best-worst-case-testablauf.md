# Best Case und Worst Case testen

Diese Datei beschreibt einen einfachen Testablauf für den .NET-Teil.

Ziel:

- prüfen, ob Warehouse, Invoice und Audit zusammen funktionieren
- Java-Service-Verhalten simulieren
- Best Case und Worst Case nachvollziehbar zeigen



Wichtig:

- Shop/Billing wird in diesem Ablauf simuliert.
- Audit-Events von Warehouse und Invoice werden über Kafka an den Audit-Service gesendet.
- Der Audit-Service konsumiert diese Events aus Kafka und speichert sie in PostgreSQL.

## 1. Services starten

Im Projektordner:

```bash
docker compose up -d
```

Danach prüfen:

```bash
curl http://localhost:5081/health
curl http://localhost:5082/health
curl http://localhost:5083/health
```

Erwartung:

```json
{"status":"healthy"}
```

## 2. Swagger öffnen

Audit-Service:

```text
http://localhost:5081/swagger
```

Warehouse-Service:

```text
http://localhost:5082/swagger
```

Invoice-Service:

```text
http://localhost:5083/swagger
```

Admin-Frontend:

```text
http://localhost:5081/admin
```

## 3. Best Case

Ziel:

Eine Bestellung läuft erfolgreich durch.

Der Ablauf ist:

1. Shop erstellt Bestellung.
2. Warehouse reserviert Bestand.
3. Billing meldet erfolgreiche Zahlung.
4. Invoice erstellt Rechnung.
5. Warehouse committet die Reservierung.
6. Audit zeigt die komplette Timeline.

Die Audit-Events von Warehouse und Invoice laufen dabei über Kafka.

Für diesen Test verwenden wir:

```text
correlationId = 10000000-0000-4000-8000-000000000301
orderId       = 10000000-0000-4000-8000-000000000401
productId    = 22222222-2222-4222-8222-222222222222
sku          = SKU-DESK-OSLO
```

### Schritt 1: Bestellung im Audit anlegen

Swagger:

```text
Audit-Service -> POST /audit/events
```

Body:

```json
{
  "correlationId": "10000000-0000-4000-8000-000000000301",
  "eventType": "ORDER_CREATED",
  "service": "shop-service",
  "payload": {
    "orderId": "10000000-0000-4000-8000-000000000401"
  },
  "previousEventId": null,
  "actor": "shop-service",
  "statusCode": "SUCCESS"
}
```

Erwartung:

```text
201 Created
```

### Schritt 2: Bestand reservieren

Swagger:

```text
Warehouse-Service -> POST /reservations
```

Header:

```text
X-Correlation-Id: 10000000-0000-4000-8000-000000000301
```

Body:

```json
{
  "orderId": "10000000-0000-4000-8000-000000000401",
  "items": [
    {
      "productId": "22222222-2222-4222-8222-222222222222",
      "sku": "SKU-DESK-OSLO",
      "quantity": 1
    }
  ]
}
```

Erwartung:

```text
201 Created
status = Reserved
```

Wichtig: Die `reservationId` aus der Antwort kopieren. Sie wird später für den Commit gebraucht.

### Schritt 3: Zahlung erfolgreich simulieren

Einfacher Weg über Swagger:

```text
Invoice-Service -> POST /events/payment-succeeded
```

Body:

```json
{
  "correlationId": "10000000-0000-4000-8000-000000000301",
  "orderId": "10000000-0000-4000-8000-000000000401",
  "transactionId": "tx-java-sim-best-001",
  "amount": 499.0,
  "currency": "EUR",
  "customerEmail": "demo.bestcase@example.com",
  "customer": {
    "name": "Max Mustermann",
    "street": "Musterstrasse 12",
    "postalCode": "58507",
    "city": "Luedenscheid",
    "country": "Deutschland"
  },
  "items": [
    {
      "sku": "SKU-DESK-OSLO",
      "name": "Schreibtisch Oslo",
      "quantity": 1,
      "unitPrice": 499.0
    }
  ]
}
```

Erwartung:

```text
202 Accepted
```

Wichtig: Die `invoiceId` aus der Antwort kopieren.

### Optional: Zahlung über Kafka simulieren

Das ist näher an der echten Architektur. Hier wird so getan, als würde der Java Billing-Service ein Kafka-Event senden.

```bash
printf '%s\n' '{"eventId":"10000000-0000-4000-8000-000000000501","correlationId":"10000000-0000-4000-8000-000000000301","eventType":"PaymentSucceeded","service":"billing-service","timestamp":"2026-05-30T09:21:40Z","payload":{"orderId":"10000000-0000-4000-8000-000000000401","transactionId":"tx-java-sim-best-001","amount":499.00,"currency":"EUR","customerEmail":"demo.bestcase@example.com","customer":{"name":"Max Mustermann","email":"demo.bestcase@example.com","street":"Musterstrasse 12","postalCode":"58507","city":"Luedenscheid","country":"Deutschland"},"items":[{"sku":"SKU-DESK-OSLO","name":"Schreibtisch Oslo","quantity":1,"unitPrice":499.00}]},"previousEventId":null,"actor":"billing-service","statusCode":"SUCCESS"}' \
| docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic payment-succeeded
```

Danach kurz warten und die Rechnung suchen:

```bash
curl "http://localhost:5083/invoices?customerEmail=demo.bestcase@example.com"
```

### Schritt 4: Rechnung prüfen

Swagger:

```text
Invoice-Service -> GET /invoices?customerEmail=demo.bestcase@example.com
```

Erwartung:

Eine Rechnung wird gefunden.

PDF prüfen:

```text
Invoice-Service -> GET /invoices/{invoiceId}/pdf
```

Oder im Browser:

```text
http://localhost:5083/invoices/DEINE-INVOICE-ID/pdf
```

### Schritt 5: Reservierung committen

Swagger:

```text
Warehouse-Service -> POST /reservations/{reservationId}/commit
```

Header:

```text
X-Correlation-Id: 10000000-0000-4000-8000-000000000301
```

Erwartung:

```text
200 OK
status = Committed
```

### Schritt 6: Bestellung abschließen

Swagger:

```text
Audit-Service -> POST /audit/events
```

Body:

```json
{
  "correlationId": "10000000-0000-4000-8000-000000000301",
  "eventType": "ORDER_COMPLETED",
  "service": "shop-service",
  "payload": {
    "orderId": "10000000-0000-4000-8000-000000000401"
  },
  "previousEventId": null,
  "actor": "shop-service",
  "statusCode": "SUCCESS"
}
```

### Schritt 7: Timeline prüfen

Swagger:

```text
Audit-Service -> GET /audit/orders/{correlationId}
```

Oder im Browser:

```text
http://localhost:5081/audit/orders/10000000-0000-4000-8000-000000000301
```

Erwartete Events:

```text
ORDER_CREATED
WAREHOUSE_RESERVED
INVOICE_CREATION_STARTED
INVOICE_CREATED
WAREHOUSE_COMMITTED
ORDER_COMPLETED
```

Wenn diese Events sichtbar sind, ist der Best Case erfolgreich.

## 4. Worst Case: Bestand reicht nicht aus

Ziel:

Warehouse kann nicht reservieren. Das System gibt sauber einen Fehler zurück und schreibt ein Audit-Event.

Für diesen Test verwenden wir:

```text
correlationId = 10000000-0000-4000-8000-000000000302
orderId       = 10000000-0000-4000-8000-000000000402
productId    = 11111111-1111-4111-8111-111111111111
sku          = SKU-CHAIR-OAK
```

Dieses Produkt hat aktuell keinen Bestand oder zu wenig Bestand.

### Schritt 1: Bestellung im Audit anlegen

Swagger:

```text
Audit-Service -> POST /audit/events
```

Body:

```json
{
  "correlationId": "10000000-0000-4000-8000-000000000302",
  "eventType": "ORDER_CREATED",
  "service": "shop-service",
  "payload": {
    "orderId": "10000000-0000-4000-8000-000000000402"
  },
  "previousEventId": null,
  "actor": "shop-service",
  "statusCode": "SUCCESS"
}
```

### Schritt 2: Reservierung versuchen

Swagger:

```text
Warehouse-Service -> POST /reservations
```

Header:

```text
X-Correlation-Id: 10000000-0000-4000-8000-000000000302
```

Body:

```json
{
  "orderId": "10000000-0000-4000-8000-000000000402",
  "items": [
    {
      "productId": "11111111-1111-4111-8111-111111111111",
      "sku": "SKU-CHAIR-OAK",
      "quantity": 1
    }
  ]
}
```

Erwartung:

```text
409 Conflict
Insufficient stock
```

### Schritt 3: Timeline prüfen

Swagger:

```text
Audit-Service -> GET /audit/orders/{correlationId}
```

Oder im Browser:

```text
http://localhost:5081/audit/orders/10000000-0000-4000-8000-000000000302
```

Erwartete Events:

```text
ORDER_CREATED
WAREHOUSE_OUT_OF_STOCK
```

Wichtig:

In diesem Fehlerfall soll keine Rechnung entstehen. Es gibt auch keinen Commit, weil keine Reservierung erzeugt wurde.

## 5. Was du in der Präsentation sagen kannst

Best Case:

> Der Shop-Service startet die Bestellung. Warehouse reserviert synchron per REST. Danach meldet Billing eine erfolgreiche Zahlung. Invoice reagiert darauf und erstellt die Rechnung. Audit speichert alle Schritte über dieselbe correlationId.

Worst Case:

> Wenn der Bestand nicht reicht, antwortet Warehouse mit 409 Conflict. Der Fehler wird im Audit gespeichert. Dadurch kann man später genau nachvollziehen, warum die Bestellung nicht abgeschlossen wurde.

## 6. Grafana prüfen

In Grafana kannst du nach diesen Werten suchen:

```text
10000000-0000-4000-8000-000000000301
10000000-0000-4000-8000-000000000302
WAREHOUSE_RESERVED
WAREHOUSE_OUT_OF_STOCK
INVOICE_CREATED
```

Damit sieht man die Logs zu genau diesem Ablauf.
