# Diagramme für die .NET-Services

Dieses Dokument zeigt den .NET-Teil des Projekts mit einfachen Mermaid-Diagrammen.

Die Diagramme sollen helfen, den Code schneller zu verstehen.

## 1. Überblick

```mermaid
flowchart LR
    Shop["Shop Service\nJava"]
    Billing["Billing Service\nJava"]
    Warehouse["Warehouse Service\n.NET"]
    Invoice["Invoice Service\n.NET"]
    Audit["Audit Service\n.NET"]
    Kafka["Kafka"]
    WarehouseDb[("PostgreSQL\nWarehouse")]
    AuditDb[("PostgreSQL\nAudit")]
    InvoiceFiles[("PDF + JSON\nInvoice Files")]

    Shop -->|"REST\nReservierung / Commit / Cancel"| Warehouse
    Billing -->|"Kafka\nPaymentSucceeded"| Kafka
    Kafka -->|"PaymentSucceeded"| Invoice
    Kafka -->|"Audit Events"| Audit

    Warehouse -->|"REST\nPOST /audit/events"| Audit
    Invoice -->|"REST\nPOST /audit/events"| Audit

    Warehouse --> WarehouseDb
    Audit --> AuditDb
    Invoice --> InvoiceFiles
```

Wichtig:

- Warehouse ist synchron über REST.
- Invoice ist asynchron über Kafka.
- Audit kann REST und Kafka.

## 2. Clean Architecture

```mermaid
flowchart TB
    Api["Api\nHTTP-Endpunkte und Middleware"]
    Application["Application\nUse Cases und Interfaces"]
    Domain["Domain\nModelle und Regeln"]
    Infrastructure["Infrastructure\nDB, Kafka, PDF, HTTP, Logging"]
    Program["Program.cs\nStart und Registrierung"]

    Program --> Api
    Program --> Application
    Program --> Infrastructure
    Api --> Application
    Application --> Domain
    Infrastructure --> Application
    Infrastructure --> Domain
```

Die wichtigste Regel:

```text
Domain kennt keine Datenbank, kein Kafka und kein ASP.NET Core.
```

## 3. Warehouse Service

```mermaid
flowchart LR
    Api["Warehouse Endpoints"]
    Reserve["ReserveStockUseCase"]
    Commit["CommitReservationUseCase"]
    Cancel["CancelReservationUseCase"]
    Inventory["Inventory Use Cases"]
    Repo["IWarehouseRepository"]
    Audit["IAuditSink"]
    Db["PostgreSQL"]

    Api --> Reserve
    Api --> Commit
    Api --> Cancel
    Api --> Inventory
    Reserve --> Repo
    Commit --> Repo
    Cancel --> Repo
    Inventory --> Repo
    Reserve --> Audit
    Commit --> Audit
    Cancel --> Audit
    Repo --> Db
```

Der Service verwaltet Bestand und Reservierungen.

## 4. Invoice Service

```mermaid
flowchart LR
    Kafka["Kafka\npayment-succeeded"]
    Consumer["PaymentSucceededKafkaConsumer"]
    UseCase["CreateInvoiceUseCase"]
    Pdf["PDF Writer"]
    Repo["Invoice Repository\nFile Metadata"]
    Audit["Audit Sink"]

    Kafka --> Consumer
    Consumer --> UseCase
    UseCase --> Pdf
    UseCase --> Repo
    UseCase --> Audit
```

Der Consumer enthält nicht die Business-Logik. Er ruft den Use Case auf.

## 5. Audit Service

```mermaid
flowchart LR
    Rest["POST /audit/events"]
    Kafka["Kafka\naudit-events"]
    Consumer["AuditEventKafkaConsumer"]
    UseCase["AppendAuditSnapshotUseCase"]
    Repo["IAuditSnapshotRepository"]
    Db["PostgreSQL"]
    Query["GET /audit/orders/{correlationId}"]

    Rest --> UseCase
    Kafka --> Consumer
    Consumer --> UseCase
    UseCase --> Repo
    Repo --> Db
    Query --> Repo
```

REST und Kafka nutzen am Ende denselben Use Case.

## 6. Reservierung im Warehouse

```mermaid
sequenceDiagram
    autonumber
    participant Shop as Shop Service
    participant Api as Warehouse API
    participant UseCase as ReserveStockUseCase
    participant Db as PostgreSQL
    participant Audit as Audit Service

    Shop->>Api: POST /reservations
    Api->>UseCase: Reservierung ausführen
    UseCase->>Db: Bestand prüfen und reservieren

    alt genug Bestand
        Db-->>UseCase: Reservierung erstellt
        UseCase->>Audit: WAREHOUSE_RESERVED
        Api-->>Shop: 201 Created
    else zu wenig Bestand
        Db-->>UseCase: nicht möglich
        UseCase->>Audit: WAREHOUSE_RESERVATION_REJECTED
        Api-->>Shop: 409 Conflict
    end
```

## 7. Rechnung über Kafka

```mermaid
sequenceDiagram
    autonumber
    participant Billing as Billing Service
    participant Kafka as Kafka
    participant Consumer as Invoice Kafka Consumer
    participant UseCase as CreateInvoiceUseCase
    participant Files as PDF/Metadata
    participant Audit as Audit Service

    Billing->>Kafka: PaymentSucceeded
    Kafka->>Consumer: Event liefern
    Consumer->>UseCase: Rechnung erstellen
    UseCase->>Files: PDF und Metadaten speichern
    UseCase->>Audit: INVOICE_CREATED
```

## 8. Audit Timeline

```mermaid
sequenceDiagram
    autonumber
    participant User as Entwickler / Admin UI
    participant Api as Audit API
    participant UseCase as GetAuditTimelineUseCase
    participant Db as PostgreSQL

    User->>Api: GET /audit/orders/{correlationId}
    Api->>UseCase: Timeline abfragen
    UseCase->>Db: Events nach correlationId suchen
    Db-->>UseCase: Events sortiert zurückgeben
    UseCase-->>Api: Timeline
    Api-->>User: 200 OK
```

## Was man aus den Diagrammen mitnehmen soll

- Warehouse bleibt REST, weil der Shop sofort eine Antwort braucht.
- Invoice nutzt Kafka, weil Rechnungserstellung im Hintergrund passieren kann.
- Audit sammelt Events zentral.
- Die `correlationId` verbindet den ganzen Ablauf.
- Business-Logik liegt in Use Cases, nicht in Endpoints oder Kafka-Consumern.
