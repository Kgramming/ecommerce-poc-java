# Y3L Supply Co. — E-commerce POC (Java / Spring Boot)

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vue.js&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-8-47A248?logo=mongodb&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.15-005571?logo=elasticsearch&logoColor=white)

The **Java / Spring Boot port** of the Y3L Supply Co. e-commerce POC — the same
frozen API, the same Vue storefront, the same data stack as the Python reference.
Proof that the architecture is language-agnostic: swap the backend, keep everything else.

This is **stage 2** of the polyglot chain:

| Stage | Language | Repo |
|---|---|---|
| 1 | Python / FastAPI (reference) | [`ecommerce-poc-python`](https://github.com/Kgramming/ecommerce-poc-python) |
| 2 | Java / Spring Boot | this repo |
| 3 | .NET 10 | coming next |
| 4 | Node.js | coming next |

Runs **side-by-side** with the Python backend — Python on `:8000`, Java on `:8001`,
both serving the same UI and the same data.

## What this is

A miniature e-commerce system that proves polyglot persistence done right.
Every stage implements the **same frozen API** ([`CONTRACT.md`](CONTRACT.md)),
ships the **same Vue 3 SPA**, and must pass the **same 12/12 black-box
acceptance harness** over HTTP.

```
Vue SPA  →  HTTP  →  Spring Boot  →  MongoDB      (product catalog)
                                 →  PostgreSQL   (orders — source of truth)
                                 →  Elasticsearch (admin search — read model)
```

## Features

- **Storefront** — product catalog with photos, category filters, live search, cart drawer, dark mode
- **Checkout** — order placement with INR pricing and instant confirmation
- **Admin search** — full-text order search across customer names *and* line items, status/date/total faceted filters, revenue KPIs
- **Order management** — order detail view, PENDING → PROCESSING → SHIPPED status flow
- **Catalog admin** — create / edit / deactivate products; edits never rewrite order history (**snapshot semantics**)
- **Two sync strategies** — dual-write *and* 15s polling keep Elasticsearch in line with Postgres (spec: implement at least two)

## API

All endpoints are frozen in [`CONTRACT.md`](CONTRACT.md) — identical across every language port.

| Method & path | Store | Notes |
|---|---|---|
| `GET /api/products` | MongoDB | filters: `category`, `q`, `include_inactive` |
| `GET /api/products/:id` | MongoDB | 404 on unknown id |
| `POST /api/products` | MongoDB | → 201 |
| `PUT /api/products/:id` | MongoDB | 400 on empty patch |
| `POST /api/orders` | PostgreSQL | → 201; validates user + live catalog, snapshots title/price, one `@Transactional` |
| `GET /api/orders/:id` | PostgreSQL | source of truth, never the search copy |
| `PATCH /api/orders/:id/status` | PG + ES | refreshes the search doc; ES failure never fails the write |
| `POST /api/search/orders` | Elasticsearch | full-text + facets + KPIs |

## Design notes

- **Snapshot semantics** — at order time the backend copies product `title` + `unit_price`
  into the order. The demo proves it: historical orders still show *"Wireless Mouse"
  @ ₹4,199* after the catalog renames it to *"Wireless Mouse Pro" @ ₹4,999*.
- **One transaction** — order + line items insert atomically via `@Transactional`
  (`all-or-nothing`).
- **Dual-write with a safety net** — the ES document is indexed with
  `refresh="wait_for"` so it is searchable the moment the API returns; if ES is
  down the sale still succeeds and the `PollWorker` thread repairs the copy.
- **The nested-query trap** — `items.title` is a nested ES field; a plain
  `multi_match` silently matches nothing. Admin search wraps it in a `nested` query.
- **No ES client library** — the backend talks to Elasticsearch over plain HTTP
  (`java.net.http`) with the exact JSON bodies the reference implementation sends.

## Getting started

**Prerequisites** — JDK 17+, Maven 3.8+, Node 18+, and the data stack on localhost:
PostgreSQL `:5435`, MongoDB `:27017`, Elasticsearch `:9200`.

```bash
# seed the data stack (from the repo root)
mvn -f backend/pom.xml compile exec:java \
  -Dexec.mainClass="com.y3l.supply.seed.SeedRunner"

# build + run the API (port 8001 — Python holds 8000)
mvn -DskipTests -f backend/pom.xml package
java -jar backend/target/supply-0.1.0.jar

# frontend (port 5174, /api proxied to :8001)
cd frontend && npm install
VITE_API_URL=http://localhost:8001 npm run dev -- --port 5174

# acceptance — 12/12 on a pristine seed
python3 tools/acceptance.py --base http://localhost:8001
```

Environment overrides ([`.env.example`](.env.example)):
`PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD`, `MONGOHOST/MONGOPORT/MONGODB`,
`ESHOST/ESPORT/ESINDEX`, `SYNC_STRATEGY` (`dual_write` | `polling`).

## Repo layout

```
backend/            Spring Boot app (com.y3l.supply) + SeedRunner
frontend/           Vue 3 + Vite SPA — byte-identical copy in every language repo
seed/               Shared, language-neutral dataset: schema.sql, users.sql,
                    orders.sql, products.json, es-mapping.json
tools/acceptance.py Black-box harness (stdlib only) — 12 checks over HTTP
CONTRACT.md         Frozen API contract — identical across all ports
knowledge-bytes/    Guided code tour in STE100 Simplified Technical English
```

## 📚 Knowledge Bytes

Understand how this backend works internally through guided, code-specific
Knowledge Bytes written in ASD-STE100 Simplified Technical English — each byte
names the real source files and functions, shows actual snippets, and explains
what the code does, why it exists, and how it connects to the rest of the
project.

Read them in order — start with `01_Three_Databases.md`:

**[Explore the Knowledge Bytes →](knowledge-bytes/)**

The prompt that generates bytes in this format lives in
[`knowledge-bytes/knowledge-bytes-prompt.md`](knowledge-bytes/knowledge-bytes-prompt.md).
