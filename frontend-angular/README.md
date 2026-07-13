# Frontend Angular

Single Angular frontend for the online shop microservice project.

## Areas

- Shop: product catalog, cart, checkout, order status
- Customer: profile, addresses, own orders
- Admin: products, customers, orders, audit, invoices
- Warehouse: stock and reservation visibility

## Data Ownership

- Product and price: shop-service
- Order: shop-service
- Stock and reservations: warehouse-service
- Audit timeline: audit-service
- Invoice documents: invoice-service
- Cart and current UI state: frontend only

## Docker

The app is built as static files and served by nginx.

```bash
docker compose up -d --build frontend-angular
```

Open:

```text
http://localhost:3001
```

## API Proxy

nginx forwards API calls:

- `/api/shop/` -> `shop-service:8087`
- `/api/warehouse/` -> `warehouse-service:8080`
- `/api/audit/` -> `audit-service:8080`
- `/api/invoices/` -> `invoice-service:8080`
