# System Diagrams

This document describes the overall online shop architecture with Java and .NET services.

The diagrams use Mermaid and keep Kafka as the message broker.

## 1. System Context Diagram

```mermaid
flowchart TB
    User["User\nEnd customer placing orders"]

    subgraph Services["Online Shop Microservices"]
        Shop["Shop-Service\nJava\nAPI gateway / entry point\nAccepts orders and orchestrates the Saga"]
        Billing["Billing-Service\nJava\nHandles payments\nPayment facade to provider stubs"]
        Warehouse["Warehouse-Service\n.NET\nStock reservation, commit, cancel"]
        Invoice["Invoice-Service\n.NET\nAsynchronous invoice creation"]
        Audit["Audit-Service\n.NET + PostgreSQL\nImmutable audit event storage"]
    end

    subgraph Infrastructure["Infrastructure"]
        Kafka["Kafka\nMessage broker\nPaymentSucceeded, PaymentFailed, AuditEvents"]
        Postgres[("PostgreSQL\nAuditEvents")]
    end

    Payment["Payment-Provider Stub\nStripe / PayPal simulation"]

    User -->|"HTTP REST\nPOST /orders"| Shop
    Shop -->|"HTTP\nPOST /reservations\nPOST /reservations/{id}/commit\nPOST /reservations/{id}/cancel"| Warehouse
    Shop -->|"HTTP\nPOST /payments"| Billing
    Billing -->|"HTTP\nPayment facade\nPOST /pay"| Payment

    Billing -->|"Event\nPaymentSucceeded\nPaymentFailed"| Kafka
    Kafka -->|"consumes\nPaymentSucceeded"| Invoice

    Shop -->|"HTTP\nPOST /audit/events"| Audit
    Billing -->|"HTTP\nPOST /audit/events"| Audit
    Warehouse -->|"HTTP\nPOST /audit/events"| Audit
    Invoice -->|"HTTP\nPOST /audit/events"| Audit

    Shop -. "HTTP\nGET /audit/orders/{correlationId}" .-> Audit
    Audit -->|"DB\nAuditEvents\nINSERT only"| Postgres

    classDef java fill:#fff7ed,stroke:#f97316,color:#111827
    classDef dotnet fill:#eef7ff,stroke:#2563eb,color:#111827
    classDef infra fill:#f3f4f6,stroke:#6b7280,color:#111827
    classDef external fill:#fef2f2,stroke:#ef4444,color:#111827

    class Shop,Billing java
    class Warehouse,Invoice,Audit dotnet
    class Kafka,Postgres infra
    class User,Payment external
```

## 2. Service / Component Diagram

```mermaid
flowchart LR
    Shop["Shop-Service (Java)\n- POST /orders\n- Orchestrates order Saga\n- Calls Warehouse and Billing via REST"]

    Billing["Billing-Service (Java)\n- POST /payments\n- Payment facade\n- Publishes PaymentSucceeded / PaymentFailed"]

    Warehouse["Warehouse-Service (.NET)\n- POST /reservations\n- POST /reservations/{id}/commit\n- POST /reservations/{id}/cancel\n- Reservation, commit, cancel"]

    Invoice["Invoice-Service (.NET)\n- consumes PaymentSucceeded\n- creates invoice PDF later\n- optional GET /invoices/{orderId}"]

    Audit["Audit-Service (.NET + PostgreSQL)\n- POST /audit/events\n- GET /audit/orders/{correlationId}\n- stores correlationId, eventType, service,\n  timestamp, payload, previousEventId,\n  actor, statusCode"]

    Kafka["Kafka\n- business event topics\n- payments topic\n- PaymentSucceeded / PaymentFailed"]

    Payment["Payment-Provider Stub\nSimulated external payment API\ncalled through Billing facade"]

    Shop -->|"HTTP\nPOST /reservations\nPOST /reservations/{id}/commit\nPOST /reservations/{id}/cancel"| Warehouse
    Shop -->|"HTTP\nPOST /payments"| Billing

    Billing -->|"HTTP\nPayment facade\nPOST /pay"| Payment
    Billing -->|"Event\nPaymentSucceeded\nPaymentFailed"| Kafka
    Kafka -->|"consumed by\nPaymentSucceeded"| Invoice

    Shop -->|"HTTP\nPOST /audit/events"| Audit
    Billing -->|"HTTP\nPOST /audit/events"| Audit
    Warehouse -->|"HTTP\nPOST /audit/events"| Audit
    Invoice -->|"HTTP\nPOST /audit/events"| Audit

    Shop -. "HTTP\nGET /audit/orders/{correlationId}" .-> Audit
```

## 3. Sequence Diagram - Happy Path

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Shop as Shop-Service
    participant Warehouse as Warehouse-Service
    participant Billing as Billing-Service
    participant Payment as Payment-Provider Stub
    participant Kafka as Kafka
    participant Invoice as Invoice-Service
    participant Audit as Audit-Service

    User->>Shop: POST /orders
    Shop->>Shop: Create order and correlationId
    Shop->>Audit: POST /audit/events<br/>ORDER_CREATED

    Shop->>Warehouse: POST /reservations
    Warehouse->>Warehouse: Check stock and create reservation
    Warehouse->>Audit: POST /audit/events<br/>WAREHOUSE_RESERVED
    Warehouse-->>Shop: 201 Created<br/>reservationId

    Shop->>Billing: POST /payments
    Billing->>Audit: POST /audit/events<br/>PAYMENT_REQUESTED
    Billing->>Payment: POST /pay<br/>via payment facade
    Payment-->>Billing: Payment accepted
    Billing->>Audit: POST /audit/events<br/>PAYMENT_SUCCEEDED

    Billing->>Kafka: publish PaymentSucceeded
    Kafka-->>Invoice: consume PaymentSucceeded
    Invoice->>Invoice: Create invoice document
    Invoice->>Audit: POST /audit/events<br/>INVOICE_CREATED

    Shop->>Warehouse: POST /reservations/{id}/commit
    Warehouse->>Warehouse: Commit reserved stock
    Warehouse->>Audit: POST /audit/events<br/>WAREHOUSE_COMMITTED
    Warehouse-->>Shop: 200 OK

    Shop->>Shop: Mark order as COMPLETED
    Shop->>Audit: POST /audit/events<br/>ORDER_COMPLETED
    Shop-->>User: 201 Created / 200 OK<br/>order completed
```

## 4. Sequence Diagram - Warehouse Commit Failure

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Shop as Shop-Service
    participant Warehouse as Warehouse-Service
    participant Billing as Billing-Service
    participant Payment as Payment-Provider Stub
    participant Kafka as Kafka
    participant Invoice as Invoice-Service
    participant Audit as Audit-Service

    User->>Shop: POST /orders
    Shop->>Audit: POST /audit/events<br/>ORDER_CREATED

    Shop->>Warehouse: POST /reservations
    Warehouse->>Audit: POST /audit/events<br/>WAREHOUSE_RESERVED
    Warehouse-->>Shop: 201 Created<br/>reservationId

    Shop->>Billing: POST /payments
    Billing->>Payment: POST /pay
    Payment-->>Billing: Payment accepted
    Billing->>Audit: POST /audit/events<br/>PAYMENT_SUCCEEDED
    Billing->>Kafka: publish PaymentSucceeded

    Kafka-->>Invoice: consume PaymentSucceeded
    Invoice->>Invoice: Create invoice document
    Invoice->>Audit: POST /audit/events<br/>INVOICE_CREATED

    Shop->>Warehouse: POST /reservations/{id}/commit
    Warehouse-->>Shop: 500 / 409<br/>commit failed
    Warehouse->>Audit: POST /audit/events<br/>WAREHOUSE_COMMIT_FAILED<br/>statusCode = FAILURE

    Shop->>Billing: POST /payments/{id}/refund
    Billing->>Payment: POST /refund<br/>via payment facade
    Payment-->>Billing: Refund accepted
    Billing->>Audit: POST /audit/events<br/>PAYMENT_REFUNDED

    Shop->>Shop: Mark order as ROLLBACK_COMPLETED
    Shop->>Audit: POST /audit/events<br/>ORDER_ROLLBACK_COMPLETED
    Shop-->>User: 409 Conflict / 500<br/>rollback completed
```

## 5. Event Overview

```mermaid
flowchart TB
    Kafka["Kafka"]

    subgraph PaymentTopic["Topic: payments"]
        PaymentSucceeded["PaymentSucceeded"]
        PaymentFailed["PaymentFailed"]
    end

    subgraph AuditEvents["Audit Events sent to Audit-Service"]
        OrderCreated["ORDER_CREATED"]
        WarehouseReserved["WAREHOUSE_RESERVED"]
        PaymentRequested["PAYMENT_REQUESTED"]
        PaymentSucceededAudit["PAYMENT_SUCCEEDED"]
        InvoiceCreated["INVOICE_CREATED"]
        WarehouseCommitted["WAREHOUSE_COMMITTED"]
        OrderCompleted["ORDER_COMPLETED"]
        WarehouseCommitFailed["WAREHOUSE_COMMIT_FAILED"]
        PaymentRefunded["PAYMENT_REFUNDED"]
        RollbackCompleted["ORDER_ROLLBACK_COMPLETED"]
    end

    Kafka --> PaymentSucceeded
    Kafka --> PaymentFailed
```
