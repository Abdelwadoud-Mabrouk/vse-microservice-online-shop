# Invoice Service (.NET 10)

Dieser Service ist zuständig für Rechnungen.

Wenn eine Zahlung erfolgreich war, erstellt der Service eine Rechnung als PDF und speichert sie dauerhaft.

## Aufgabe des Service

Der Invoice-Service verarbeitet erfolgreiche Zahlungen.

Hier passiert Folgendes:

1. Ein `PaymentSucceeded` Event kommt an.
2. Der Service prüft die Daten.
3. Eine Rechnung wird erstellt.
4. Die Rechnung wird als PDF gespeichert.
5. Metadaten zur Rechnung werden gespeichert.
6. Der Service schreibt Audit-Events.

Der Service berechnet keine Preise selbst aus dem Warenkorb des Kunden. Preise müssen aus dem vertrauenswürdigen System kommen, nicht direkt vom Frontend.

## Kommunikation

Der Service unterstützt aktuell zwei Wege.

### Kafka

Das ist der Hauptweg für den echten Ablauf.

Der Service konsumiert:

```text
PaymentSucceeded
```

Topic:

```text
payment-succeeded
```

Nach dem Event wird die Rechnung erzeugt.

### REST

Für Entwicklung, Tests und Swagger gibt es zusätzlich:

```text
POST /events/payment-succeeded
```

Das ist praktisch, wenn man den Service ohne Kafka manuell testen möchte.

## Endpunkte

```text
GET  /health
POST /events/payment-succeeded
GET  /invoices?customerEmail={email}
GET  /invoices/{invoiceId}
GET  /invoices/{invoiceId}/pdf
```

Wichtig:

- `GET /invoices/{invoiceId}/pdf` lädt die PDF-Rechnung herunter.
- `GET /invoices?customerEmail=...` sucht Rechnungen über die E-Mail-Adresse.

## Beispiel für PaymentSucceeded

```json
{
  "correlationId": "00000000-0000-0000-0000-000000000000",
  "orderId": "00000000-0000-0000-0000-000000000000",
  "transactionId": "tx-123",
  "amount": 129.99,
  "currency": "EUR",
  "customerEmail": "customer@example.com",
  "customer": {
    "name": "Max Mustermann",
    "email": "customer@example.com",
    "street": "Musterstrasse 12",
    "postalCode": "58507",
    "city": "Luedenscheid",
    "country": "Deutschland"
  },
  "items": [
    {
      "sku": "SKU-CHAIR-OAK",
      "name": "Esszimmerstuhl Eiche",
      "quantity": 1,
      "unitPrice": 129.99
    }
  ]
}
```

Der Kafka-Consumer kann auch einen Envelope lesen, bei dem diese Daten in `payload` liegen.

## Persistenz

Der Service nutzt keine relationale Datenbank.

Rechnungen werden im Dateisystem gespeichert:

```text
INVOICE_DATA_DIR
  pdf/
  metadata/
```

In Docker Compose ist das ein Volume:

```text
invoice-data:/app/data/invoices
```

Dadurch bleiben PDFs auch nach einem Container-Neustart erhalten.

## Konfiguration

```env
AUDIT_SERVICE_URL=http://audit-service:8080
INVOICE_RETRY_COUNT=3
INVOICE_FORCE_FAILURE=false
INVOICE_DATA_DIR=/app/data/invoices
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_PAYMENT_SUCCEEDED_TOPIC=payment-succeeded
KAFKA_INVOICE_CONSUMER_GROUP=invoice-service
```

`INVOICE_FORCE_FAILURE=true` ist nur für Tests und Demo-Fehlerfälle gedacht.

## Logging

Der Service loggt wichtige Schritte:

- Kafka-Nachricht empfangen
- Nachricht gelesen
- Rechnungserstellung gestartet
- Rechnung erfolgreich gespeichert
- Fehler bei Validierung oder Erstellung
- Audit-Event geschrieben

Wichtige Felder:

- `service`
- `correlationId`
- `operation`
- `eventType`
- `orderId`
- `status`
- `durationMs`

Damit kann man in Grafana sehen, ob die Rechnungserstellung erfolgreich war und wo ein Fehler passiert ist.

## Lokal starten

Aus dem Repository-Root:

```bash
docker compose up -d --build invoice-service
```

Oder direkt:

```bash
/usr/local/share/dotnet/dotnet run --project invoice-service-dotnet/src/InvoiceService/InvoiceService.csproj
```

## Tests

```bash
/usr/local/share/dotnet/dotnet test invoice-service-dotnet/tests/InvoiceService.Tests/InvoiceService.Tests.csproj
```

## Aktuell umgesetzt

- Rechnungserstellung aus `PaymentSucceeded`.
- Kafka-Consumer für erfolgreiche Zahlungen.
- REST-Endpunkt für manuelles Testen.
- PDF-Erzeugung.
- Dauerhafte Ablage von PDF und Metadaten.
- Suche nach E-Mail-Adresse.
- Strukturierte Logs.
- Audit-Events für wichtige Schritte.
- Tests für Domain, Use Case und Kafka-Deserializer.

## Noch offen

- Finales Event-Format mit Billing-Service abstimmen.
- Optional später eine relationale Invoice-Datenbank ergänzen.
- Weitere PDF-Details ergänzen, falls die Fachanforderung wächst.
