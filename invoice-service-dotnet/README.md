# Invoice-Service

## Aufgabe

Der Invoice-Service erstellt nach einer erfolgreichen Zahlung eine
PDF-Rechnung. Wiederholte Events erzeugen keine zweite Rechnung.

## Technologie

- .NET 10
- ASP.NET Core Minimal APIs
- Apache Kafka
- PDF- und Metadatenspeicher

## API

```text
GET  /health
POST /events/payment-succeeded
GET  /invoices?customerEmail={email}
GET  /invoices/{invoiceId}
GET  /invoices/{invoiceId}/pdf
```

Die vollständige OpenAPI-Beschreibung ist im laufenden Docker-Stack unter
`http://localhost:5083/swagger` verfügbar.

## Kafka-Events

Der Service konsumiert `PAYMENT_SUCCEEDED` aus dem Topic `billing`. Er meldet
das Ergebnis als:

- `INVOICE_CREATED`
- `INVOICE_CREATION_FAILED`

Die `transactionId` verhindert doppelte Rechnungen.

## Datenbank

Ein eigener Dateispeicher enthält PDF-Dateien und Metadaten. Kein anderer
Service greift direkt auf diesen Speicher zu.

## Konfiguration

Wichtige Werte sind Kafka-Broker, Topic, Audit-URL, Retry-Anzahl und
Speicherverzeichnis. `INVOICE_RETRY_COUNT=3` aktiviert insgesamt drei
Erstellungsversuche. Nach dem letzten Fehler dokumentiert der Service
`INVOICE_CREATION_FAILED` im Audit-Service.

## Docker

```bash
docker compose up -d --build invoice-service
```

Das Volume `invoice-data` erhält Rechnungen über Container-Neustarts hinweg.

## Tests

```bash
dotnet test invoice-service-dotnet/tests/InvoiceService.Tests/InvoiceService.Tests.csproj
```
