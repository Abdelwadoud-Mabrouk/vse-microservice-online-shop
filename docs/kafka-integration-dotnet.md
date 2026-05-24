# Kafka-Integration im .NET-Teil

Kafka wird im .NET-Teil für asynchrone Kommunikation verwendet.

Das bedeutet: Ein Service muss nicht direkt auf einen anderen Service warten. Er veröffentlicht oder empfängt ein Event.

## Welche .NET-Services nutzen Kafka?

Aktuell nutzen zwei .NET-Services Kafka:

- `invoice-service-dotnet`
- `audit-service-dotnet`

Der `warehouse-service-dotnet` bleibt REST-basiert. Das ist bewusst so.

## Warum bleibt Warehouse REST?

Der Shop muss direkt wissen, ob Produkte verfügbar sind.

Wenn der Kunde bestellt, muss der Bestand sofort geprüft werden. Deshalb ist REST hier einfacher und passender.

```text
Shop -> Warehouse -> direkte Antwort
```

## Warum nutzt Invoice Kafka?

Eine Rechnung muss nicht sofort im gleichen HTTP-Request erstellt werden.

Wenn die Zahlung erfolgreich war, reicht ein Event:

```text
PaymentSucceeded
```

Der Invoice-Service empfängt dieses Event und erstellt danach die Rechnung.

Der Vorteil ist:

- Billing und Invoice sind entkoppelt.
- Die Rechnungserstellung kann im Hintergrund passieren.
- Fehler lassen sich sauber loggen.

## Warum nutzt Audit Kafka?

Audit-Events sollen zentral gesammelt werden.

Ein Service kann ein Event senden, und der Audit-Service speichert es.

So kann man später nachvollziehen, was im System passiert ist.

## Event Envelope

Für Kafka wird ein einfacher Envelope unterstützt.

Beispiel:

```json
{
  "eventId": "00000000-0000-0000-0000-000000000000",
  "correlationId": "00000000-0000-0000-0000-000000000000",
  "eventType": "PaymentSucceeded",
  "service": "billing-service",
  "timestamp": "2026-05-23T07:30:00Z",
  "payload": {},
  "previousEventId": null,
  "actor": "billing-service",
  "statusCode": "SUCCESS"
}
```

Die `correlationId` ist besonders wichtig. Damit kann man alle Events einer Bestellung zusammenfinden.

## Topics

Aktuell werden diese Topics verwendet:

```text
payment-succeeded
audit-events
```

Konfiguration:

```env
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_PAYMENT_SUCCEEDED_TOPIC=payment-succeeded
KAFKA_AUDIT_EVENTS_TOPIC=audit-events
KAFKA_INVOICE_CONSUMER_GROUP=invoice-service
KAFKA_AUDIT_CONSUMER_GROUP=audit-service
```

## Wo liegt der Kafka-Code?

Invoice:

```text
invoice-service-dotnet/src/InvoiceService/Infrastructure/Kafka/
```

Audit:

```text
audit-service-dotnet/src/AuditService/Infrastructure/Kafka/
```

Das ist wichtig: Kafka ist ein technisches Detail. Deshalb liegt es in `Infrastructure`.

Die Business-Logik liegt nicht im Kafka-Consumer.

## Ablauf bei PaymentSucceeded

```text
Kafka Topic payment-succeeded
  -> Invoice Kafka Consumer
  -> PaymentSucceededEventDeserializer
  -> CreateInvoiceUseCase
  -> PDF speichern
  -> Audit-Event schreiben
```

## Ablauf bei Audit-Events

```text
Kafka Topic audit-events
  -> Audit Kafka Consumer
  -> AuditEventEnvelopeDeserializer
  -> AppendAuditSnapshotUseCase
  -> PostgreSQL speichern
```

## Fehlerbehandlung

Kaputte Kafka-Nachrichten sollen den Service nicht stoppen.

Wenn eine Nachricht nicht gelesen werden kann:

- Der Fehler wird strukturiert geloggt.
- Der Status ist z.B. `VALIDATION_FAILED`.
- Der Service läuft weiter.

Das ist für Demo und Entwicklung wichtig, weil ein schlechtes Event nicht den ganzen Service kaputtmachen soll.

## Tests

Es gibt Tests für die Deserializer.

Geprüft wird:

- gültige Envelope-Nachrichten
- direkte Payload-Nachrichten
- fehlerhafte JSON-Nachrichten
- fehlende Pflichtfelder

## Aktuell umgesetzt

- Kafka in Docker Compose.
- Kafka-Consumer für Invoice.
- Kafka-Consumer für Audit.
- Konfiguration über Umgebungsvariablen.
- Strukturierte Logs rund um Kafka.
- Fehlerbehandlung für ungültige Nachrichten.
- Tests für Kafka-Deserializer.

## Noch offen

- Finales Event-Format mit Java/Billing abstimmen.
- Topics später explizit anlegen statt nur Auto-Create lokal zu nutzen.
- Optional Dead-Letter-Topic für fehlerhafte Events ergänzen.
