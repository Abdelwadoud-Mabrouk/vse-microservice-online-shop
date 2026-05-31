# .NET E2E-Tests

Dieses Testprojekt prüft den kompletten .NET-Ablauf gegen laufende Docker-Container.

Es ersetzt nicht die Unit Tests der einzelnen Services. Es ergänzt sie.

## Was wird geprüft?

- Audit-Service ist erreichbar
- Warehouse-Service ist erreichbar
- Invoice-Service ist erreichbar
- Warehouse reserviert Bestand per REST
- Invoice verarbeitet `PaymentSucceeded` über Kafka
- Invoice erzeugt eine Rechnung und ein PDF
- Audit speichert Events aus Kafka
- Worst Case: zu wenig Bestand
- Worst Case: ungültiges Payment-Event

## Start

Zuerst müssen die Container laufen:

```bash
docker compose up -d
```

Dann:

```bash
dotnet test dotnet-e2e-tests/OnlineShop.DotNetServices.E2E.Tests.csproj
```

Oder alles zusammen über die Solution:

```bash
dotnet test OnlineShop.DotNetServices.sln
```

## Konfiguration

Standardwerte:

```text
Audit:     http://localhost:5081
Warehouse: http://localhost:5082
Invoice:   http://localhost:5083
Kafka:     localhost:9092
```

Man kann sie überschreiben:

```bash
E2E_AUDIT_URL=http://localhost:5081 \
E2E_WAREHOUSE_URL=http://localhost:5082 \
E2E_INVOICE_URL=http://localhost:5083 \
E2E_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
dotnet test dotnet-e2e-tests/OnlineShop.DotNetServices.E2E.Tests.csproj
```

## Wichtig

Diese Tests sind echte Integrationstests.

Wenn Docker nicht läuft, schlagen sie fehl. Das ist gewollt, weil genau der echte Ablauf geprüft wird.
