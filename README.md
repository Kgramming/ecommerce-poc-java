# Y3L Supply Co. — E-commerce POC (Java / Spring Boot)

Stage 2 of the polyglot e-commerce POC. Stage 1 (the golden reference) is the
Python/FastAPI build: [`Kgramming/ecommerce-poc-python`](https://github.com/Kgramming/ecommerce-poc-python).
The API surface is frozen in [`CONTRACT.md`](CONTRACT.md) and is identical across
all four language ports.

## Architecture

| Concern | Store |
|---|---|
| Product catalog (Screen 1 + 5) | MongoDB |
| Orders — source of truth (Screen 2 + 4) | PostgreSQL |
| Admin search — read model (Screen 3) | Elasticsearch |
| Storefront + admin UI | Vue 3 SPA in `frontend/` (shared verbatim across ports) |

Order placement validates against the **live** MongoDB catalog, snapshots
title + unit price into Postgres inside **one transaction**, then syncs the ES
read model via **dual-write** (with a **polling** worker as strategy B).

## Prerequisites

- JDK 17+, Maven 3.8+
- The data stack on `localhost`: PostgreSQL `:5435`, MongoDB `:27017`,
  Elasticsearch `:9200` (see `~/ecommerce-poc/docker-compose.yml` on the demo Mac)

## Run

```bash
# 1. Seed the data stack (from the repo root)
mvn -q -f backend/pom.xml compile exec:java \
  -Dexec.mainClass="com.y3l.supply.seed.SeedRunner"

# 2. Start the API (port 8000)
mvn -f backend/pom.xml spring-boot:run

# 3. Start the frontend (port 5173)
cd frontend && npm install && npm run dev
```

Environment overrides: `PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD`,
`MONGOHOST/MONGOPORT/MONGODB`, `ESHOST/ESPORT/ESINDEX`, `SYNC_STRATEGY`.

## Verify

```bash
python3 tools/acceptance.py   # 12/12 expected on a pristine seed
```
