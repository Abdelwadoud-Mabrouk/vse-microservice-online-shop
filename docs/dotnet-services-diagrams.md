# .NET Services - Diagrams

This document explains the .NET part of the online shop system:

- `warehouse-service-dotnet`
- `invoice-service-dotnet`
- `audit-service-dotnet`

The diagrams use Mermaid so they can be rendered directly in GitHub and most Markdown editors.

## 1. Service Context

```mermaid
flowchart LR
    Shop["Shop Service\n(external API / colleague)"]
    Billing["Billing Service\n(payment flow / colleague)"]
    Warehouse["Warehouse Service\n.NET"]
    Invoice["Invoice Service\n.NET"]
    Audit["Audit Service\n.NET"]
    Postgres[("PostgreSQL\nAudit Snapshots")]
    InvoiceFiles[("Invoice Files\nJSON/PDF later")]

    Shop -->|"POST /reservations\nDELETE /reservations/{id}\nPOST /reservations/{id}/commit"| Warehouse
    Billing -->|"PaymentSucceeded event\ncurrently HTTP"| Invoice

    Warehouse -->|"POST /audit/events"| Audit
    Invoice -->|"POST /audit/events"| Audit
    Shop -. "should also write audit events" .-> Audit
    Billing -. "should also write audit events" .-> Audit

    Audit --> Postgres
    Invoice --> InvoiceFiles
```

## 2. Clean Architecture Shape

Each .NET service follows the same pragmatic structure.

```mermaid
flowchart TB
    Api["Api\nHTTP requests, middleware, endpoint mapping"]
    Program["Program.cs\nDI wiring and routes"]
    Application["Application\nUse cases and interfaces"]
    Domain["Domain\nEntities and business rules"]
    Infrastructure["Infrastructure\nDB, files, HTTP clients, in-memory storage"]

    Program --> Api
    Program --> Application
    Program --> Infrastructure
    Api --> Application
    Application --> Domain
    Infrastructure --> Application
    Infrastructure --> Domain

    classDef inside fill:#eef7ff,stroke:#2563eb,color:#111827
    classDef outside fill:#fff7ed,stroke:#f97316,color:#111827

    class Domain,Application inside
    class Api,Program,Infrastructure outside
```

Important rule: `Domain` and `Application` do not depend on ASP.NET Core, PostgreSQL, Docker, Swagger, HTTP, or file storage.

## 3. Warehouse Components

```mermaid
flowchart LR
    subgraph API["Api"]
        ReservationRequest["CreateReservationRequest"]
        Correlation["CorrelationIdMiddleware"]
    end

    subgraph APP["Application"]
        GetInventory["GetInventoryUseCase"]
        Reserve["ReserveStockUseCase"]
        Cancel["CancelReservationUseCase"]
        Commit["CommitReservationUseCase"]
        RepoPort["IWarehouseRepository"]
        AuditPort["IAuditSink"]
    end

    subgraph DOMAIN["Domain"]
        Reservation["Reservation"]
        Item["ReservationItem"]
        Status["ReservationStatus"]
    end

    subgraph INFRA["Infrastructure"]
        MemoryRepo["InMemoryWarehouseRepository"]
        AuditHttp["HttpAuditSink"]
    end

    ReservationRequest --> Reserve
    Correlation --> Reserve
    GetInventory --> RepoPort
    Reserve --> RepoPort
    Cancel --> RepoPort
    Commit --> RepoPort
    Reserve --> AuditPort
    Cancel --> AuditPort
    Commit --> AuditPort

    RepoPort --> Reservation
    Reservation --> Item
    Reservation --> Status

    MemoryRepo -.implements.-> RepoPort
    AuditHttp -.implements.-> AuditPort
```

## 4. Invoice Components

```mermaid
flowchart LR
    subgraph API["Api"]
        PaymentRequest["PaymentSucceededRequest"]
        Correlation["CorrelationIdMiddleware"]
    end

    subgraph APP["Application"]
        CreateInvoice["CreateInvoiceUseCase"]
        GetInvoice["GetInvoiceUseCase"]
        InvoiceRepoPort["IInvoiceRepository"]
        DocumentPort["IInvoiceDocumentWriter"]
        AuditPort["IAuditSink"]
    end

    subgraph DOMAIN["Domain"]
        Payment["PaymentSucceeded"]
        Invoice["Invoice"]
        LineItem["InvoiceLineItem"]
    end

    subgraph INFRA["Infrastructure"]
        MemoryRepo["InMemoryInvoiceRepository"]
        JsonWriter["JsonInvoiceDocumentWriter"]
        AuditHttp["HttpAuditSink"]
    end

    PaymentRequest --> Payment
    PaymentRequest --> CreateInvoice
    Correlation --> CreateInvoice
    CreateInvoice --> Payment
    CreateInvoice --> Invoice
    CreateInvoice --> InvoiceRepoPort
    CreateInvoice --> DocumentPort
    CreateInvoice --> AuditPort
    GetInvoice --> InvoiceRepoPort
    Invoice --> LineItem

    MemoryRepo -.implements.-> InvoiceRepoPort
    JsonWriter -.implements.-> DocumentPort
    AuditHttp -.implements.-> AuditPort
```

## 5. Audit Components

```mermaid
flowchart LR
    subgraph API["Api"]
        AuditRequest["CreateAuditSnapshotRequest"]
    end

    subgraph APP["Application"]
        Append["AppendAuditSnapshotUseCase"]
        Timeline["GetAuditTimelineUseCase"]
        RepoPort["IAuditSnapshotRepository"]
    end

    subgraph DOMAIN["Domain"]
        Snapshot["AuditSnapshot"]
    end

    subgraph INFRA["Infrastructure"]
        PostgresRepo["PostgresAuditSnapshotRepository"]
        Postgres[("PostgreSQL")]
    end

    AuditRequest --> Append
    Append --> Snapshot
    Append --> RepoPort
    Timeline --> RepoPort
    PostgresRepo -.implements.-> RepoPort
    PostgresRepo --> Postgres
```

## 6. Warehouse Reservation Flow

```mermaid
sequenceDiagram
    autonumber
    participant Shop as Shop Service
    participant WHApi as Warehouse API
    participant UseCase as ReserveStockUseCase
    participant Repo as IWarehouseRepository
    participant Audit as Audit Service

    Shop->>WHApi: POST /reservations<br/>X-Correlation-Id
    WHApi->>WHApi: validate request
    WHApi->>UseCase: Execute(command)
    UseCase->>Repo: TryReserve(orderId, items)

    alt stock available
        Repo-->>UseCase: reservation created
        UseCase->>Audit: POST /audit/events<br/>WAREHOUSE_RESERVED
        UseCase-->>WHApi: success
        WHApi-->>Shop: 201 Created
    else out of stock
        Repo-->>UseCase: failure
        UseCase->>Audit: POST /audit/events<br/>WAREHOUSE_OUT_OF_STOCK
        UseCase-->>WHApi: failure
        WHApi-->>Shop: 409 Conflict
    end
```

## 7. Warehouse Compensation Flow

This is used when payment fails and the already reserved stock must be released.

```mermaid
sequenceDiagram
    autonumber
    participant Shop as Shop Service
    participant WHApi as Warehouse API
    participant UseCase as CancelReservationUseCase
    participant Repo as IWarehouseRepository
    participant Audit as Audit Service

    Shop->>WHApi: DELETE /reservations/{reservationId}<br/>X-Correlation-Id
    WHApi->>UseCase: Execute(reservationId)
    UseCase->>Repo: Cancel(reservationId)

    alt reservation exists and is reserved
        Repo-->>UseCase: cancelled reservation
        UseCase->>Audit: POST /audit/events<br/>WAREHOUSE_RESERVATION_CANCELLED
        WHApi-->>Shop: 200 OK
    else reservation not found or already closed
        Repo-->>UseCase: null
        WHApi-->>Shop: 404 Not Found
    end
```

## 8. Invoice Creation With Retry

```mermaid
sequenceDiagram
    autonumber
    participant Billing as Billing Service
    participant InvApi as Invoice API
    participant UseCase as CreateInvoiceUseCase
    participant Writer as IInvoiceDocumentWriter
    participant Repo as IInvoiceRepository
    participant Audit as Audit Service

    Billing->>InvApi: POST /events/payment-succeeded<br/>X-Correlation-Id
    InvApi->>InvApi: validate event
    InvApi->>UseCase: Execute(payment, retryCount=3)
    UseCase->>Audit: POST /audit/events<br/>INVOICE_CREATION_STARTED

    loop up to 3 attempts
        UseCase->>Writer: Write(invoice)
        alt write succeeds
            Writer-->>UseCase: document path
            UseCase->>Repo: Save(invoice)
            UseCase->>Audit: POST /audit/events<br/>INVOICE_CREATED
            UseCase-->>InvApi: success
            InvApi-->>Billing: 202 Accepted
        else write fails
            Writer-->>UseCase: exception
            UseCase->>UseCase: wait briefly before retry
        end
    end

    opt all attempts failed
        UseCase->>Audit: POST /audit/events<br/>INVOICE_CREATION_FAILED
        InvApi-->>Billing: 503 Service Unavailable
    end
```

## 9. Audit Timeline Query

```mermaid
sequenceDiagram
    autonumber
    participant User as User / Developer
    participant AuditApi as Audit API
    participant UseCase as GetAuditTimelineUseCase
    participant Repo as IAuditSnapshotRepository
    participant Db as PostgreSQL

    User->>AuditApi: GET /audit/orders/{correlationId}
    AuditApi->>UseCase: Execute(correlationId)
    UseCase->>Repo: FindByCorrelationId(correlationId)
    Repo->>Db: SELECT snapshots ordered by timestamp
    Db-->>Repo: snapshot rows
    Repo-->>UseCase: AuditSnapshot list
    UseCase-->>AuditApi: AuditSnapshotDto list
    AuditApi-->>User: 200 OK timeline
```

## 10. Audit Snapshot Data Model

```mermaid
erDiagram
    AUDIT_SNAPSHOTS {
        uuid id PK
        uuid correlation_id
        varchar event_type
        varchar service
        timestamptz timestamp_utc
        jsonb payload
        uuid previous_event_id
        varchar actor
        varchar status_code
    }

    AUDIT_SNAPSHOTS ||--o| AUDIT_SNAPSHOTS : "previous_event_id"
```

The table is append-only from the application perspective. Every state change is represented by a new row.

## 11. Current Integration Status

```mermaid
flowchart TB
    Done["Implemented now"]
    Later["Still to integrate with colleague's services"]

    Done --> A["Warehouse REST API"]
    Done --> B["Invoice HTTP event endpoint"]
    Done --> C["Audit REST API + PostgreSQL"]
    Done --> D["Clean Architecture folders"]
    Done --> E["Swagger UI"]

    Later --> F["Shop calls Warehouse"]
    Later --> G["Billing sends PaymentSucceeded"]
    Later --> H["RabbitMQ event transport"]
    Later --> I["Full order Saga orchestration"]
```
