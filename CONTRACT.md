# API Contract — E-commerce POC (FROZEN)

This contract is **identical for all 4 backend ports** (Python → Java → .NET 10 → Node.js).
A port is correct only if it satisfies this contract byte-for-byte in behavior.
Changes require updating this doc + the acceptance harness + all repos, same day.

Base URL: `http://localhost:8000` · All bodies/ responses: JSON · Errors: `{"detail": "..."}`

---

## Conventions
- Money: JSON numbers with 2 decimals (e.g. `50.16`).
- Timestamps: ISO-8601 UTC (`2026-07-09T19:00:00Z`).
- Order statuses (exact uppercase everywhere): `PENDING`, `PROCESSING`, `SHIPPED`.
- Product `id` = MongoDB ObjectId as string.

## Product (MongoDB)
```json
{
  "id": "64f1a2b3c4d5e6f7a8b9c0d1",
  "sku": "WM-001",
  "title": "Wireless Mouse",
  "description": "Ergonomic 2.4GHz mouse",
  "price": 50.16,
  "category": "peripherals",
  "tags": ["wireless", "usb", "office"],
  "attributes": { "color": "black", "dpi": 1600, "battery": "AA" },
  "variants": [{ "sku": "WM-001-BLK", "color": "black", "stock": 40 }],
  "active": true,
  "updated_at": "2026-09-25T10:00:00Z"
}
```

## Endpoints

### `GET /api/products` → 200 (MongoDB only)
Query params: `?category=audio` (case-insensitive) `?q=wireless` (optional filters).
Returns only `active: true` products, unless `?include_inactive=true` is passed
(used by the catalog admin).
```json
{ "products": [ { "<product>" }, ... ], "total": 24 }
```

### `GET /api/products/:id` → 200 | 404 (MongoDB only)
Returns one product object.

### `POST /api/products` → 201 | 400 (MongoDB only)
Body: product object without `id`/`updated_at`. Creates the document.

### `PUT /api/products/:id` → 200 | 404 | 400 (MongoDB only)
Body: partial or full product fields. Must NOT rewrite historical orders.

### `POST /api/orders` → 201 | 400 | 404 (PostgreSQL transaction)
```json
// request
{ "user_id": 1, "items": [ { "product_id": "64f1a2b3c4d5e6f7a8b9c0d1", "quantity": 2 } ] }
// response 201 — title/unit_price are snapshots taken at order time
{
  "id": 41, "user_id": 1,
  "customer": { "id": 1, "name": "Aarav Sharma", "email": "aarav.sharma@example.com" },
  "order_date": "2026-10-15T19:00:00Z", "status": "PENDING", "total_amount": 8398.00,
  "items": [ { "product_id": "64f1a2b3c4d5e6f7a8b9c0d1", "title": "Wireless Mouse",
               "quantity": 2, "unit_price": 4199.00 } ]
}
```
Rules: validate every `product_id` exists and is active (from MongoDB); snapshot title+price;
insert 1 order + N items in ONE Postgres transaction — any failure rolls back everything (400);
on commit, trigger the ES sync.

### `GET /api/orders/:id` → 200 | 404 (PostgreSQL only — source of truth)
Same shape as the POST response.

### `PATCH /api/orders/:id/status` → 200 | 400 | 404 (PostgreSQL + ES update)
```json
// request
{ "status": "SHIPPED" }
// response: the updated order (same shape as above)
```
Updates Postgres, then updates the ES document so search stays accurate.

### `POST /api/search/orders` → 200 (Elasticsearch only)
```json
// request
{
  "query": "wireless",
  "statuses": ["PENDING", "SHIPPED"],
  "date_from": "2026-08-01", "date_to": "2026-10-15",
  "min_total": 5000, "max_total": 20000,
  "page": 1, "page_size": 20
}
// response
{
  "total": 12,
  "orders": [
    { "order_id": 5, "order_date": "2026-07-09T19:00:00Z", "status": "PENDING",
      "total_amount": 8398.00,
      "customer": { "id": 3, "name": "Kabir Malhotra", "email": "kabir.malhotra@example.com" } }
  ],
  "kpis": {
    "total_revenue": 123456.00,
    "count_by_status": { "PENDING": 5, "PROCESSING": 4, "SHIPPED": 3 }
  }
}
```
Rules: `query` is a multi-match over customer name + item titles (partial text must match —
"kabir" hits customer "Kabir Malhotra", "wireless" hits "Wireless Mouse" items). All filters AND together.
KPIs are computed over the filtered set. Never touches Postgres/MongoDB. All money is in INR (₹).

## Status codes
- `200` ok · `201` created · `400` bad request / validation / transaction failure
- `404` not found · `422` invalid status value · `500` unexpected (with `{"detail"}`)
