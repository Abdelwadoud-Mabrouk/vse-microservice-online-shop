# Warehouse Service (.NET 10)

Dieser Service ist zuständig für den Lagerbestand.

Er verwaltet Produkte, verfügbare Mengen und Reservierungen für Bestellungen.

## Aufgabe des Service

Der Warehouse-Service beantwortet die Frage:

```text
Sind die gewünschten Produkte verfügbar?
```

Wenn genug Bestand vorhanden ist, legt der Service eine Reservierung an.

Später kann diese Reservierung:

- bestätigt werden (`commit`)
- oder abgebrochen werden (`cancel`)

Der Service verwaltet keine Preise. Preise gehören fachlich eher in Shop/Billing oder in einen eigenen Produkt-/Preis-Service. Warehouse kümmert sich nur um Bestand.

## Warum REST?

Der Warehouse-Service wird synchron per REST verwendet.

Das ist sinnvoll, weil der Shop direkt wissen muss, ob Ware verfügbar ist. Ohne diese Antwort kann der Bestellprozess nicht sauber weiterlaufen.

Darum wurde Warehouse nicht als Kafka-getriebener Service für den Hauptablauf umgesetzt.

## Endpunkte

```text
GET    /health
GET    /inventory
GET    /inventory/{productId}
POST   /inventory
PUT    /inventory/{productId}
PATCH  /inventory/{productId}/stock
DELETE /inventory/{productId}

POST   /reservations
DELETE /reservations/{reservationId}
POST   /reservations/{reservationId}/commit
```

Die Inventory-Endpunkte sind für Verwaltung und Demo hilfreich.

Die Reservation-Endpunkte sind für den Bestellprozess wichtig.

## Beispiel: Reservierung

```json
{
  "orderId": "00000000-0000-0000-0000-000000000000",
  "items": [
    {
      "productId": "11111111-1111-4111-8111-111111111111",
      "sku": "SKU-CHAIR-OAK",
      "quantity": 1
    }
  ]
}
```

Der Header `X-Correlation-Id` sollte mitgeschickt werden.

Wenn er fehlt, erzeugt der Service selbst eine neue ID.

## Datenbank

Der Service nutzt PostgreSQL.

Gespeichert werden:

- Produkte
- Lagerbestand
- Reservierungen
- Reservierungspositionen

Beim Start werden Möbel-Produkte als Seed-Daten gepflegt, wenn die Tabelle leer ist.

Konfiguration:

```env
WAREHOUSE_DB_CONNECTION=Host=warehouse-db;Port=5432;Database=...;Username=...;Password=...
```

## Audit

Der Warehouse-Service schreibt wichtige Ereignisse an den Audit-Service.

Beispiele:

- `WAREHOUSE_RESERVED`
- `WAREHOUSE_RESERVATION_REJECTED`
- `WAREHOUSE_COMMITTED`
- `WAREHOUSE_CANCELLED`
- `WAREHOUSE_PRODUCT_UPDATED`

So sieht man später in der Audit-Timeline, was im Lager passiert ist.

## Logging

Der Service loggt wichtige Schritte:

- Reservierung gestartet
- Reservierung erfolgreich
- Bestand reicht nicht aus
- Reservierung bestätigt
- Reservierung abgebrochen
- Produktbestand geändert

Wichtige Felder:

- `service`
- `correlationId`
- `operation`
- `route`
- `status`
- `durationMs`
- `errorCode`

Diese Logs helfen beim Debugging in Grafana.

## Lokal starten

Aus dem Repository-Root:

```bash
docker compose up -d --build warehouse-service
```

Oder direkt:

```bash
/usr/local/share/dotnet/dotnet run --project warehouse-service-dotnet/src/WarehouseService/WarehouseService.csproj
```

## Tests

```bash
/usr/local/share/dotnet/dotnet test warehouse-service-dotnet/tests/WarehouseService.Tests/WarehouseService.Tests.csproj
```

## Aktuell umgesetzt

- Produkte und Bestand in PostgreSQL.
- Mindestens 20 Möbel-Produkte als Seed-Daten.
- Bestand reservieren.
- Reservierung abbrechen.
- Reservierung committen.
- Inventory-Verwaltung über REST.
- Audit-Events an den Audit-Service.
- Strukturierte Logs.
- Tests für Reservierungen und Inventory-Verwaltung.

## Noch offen

- Final mit Shop abstimmen, welche Felder bei Reservierungen gesendet werden.
- Bei Bedarf Pagination direkt im Warehouse-Endpunkt erweitern.
- Später eventuell ein eigenes Produkt-/Preis-System ergänzen.
