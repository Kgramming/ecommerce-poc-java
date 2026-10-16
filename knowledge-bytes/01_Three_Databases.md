# 01 — Three Databases, One Job Each

Source files in this byte: `backend/src/main/resources/application.properties`, `backend/src/main/java/com/y3l/supply/SupplyApplication.java`, `CONTRACT.md`

---

### Byte 1: Three databases, one job each

**Builds on:** None (first byte)

**In plain terms:** Y3L Supply Co. is an e-commerce demo. It uses three databases. Each database does one job. MongoDB holds the product catalog. PostgreSQL holds orders. PostgreSQL is the *source of truth*. The source of truth is the database that owns the data. Elasticsearch holds a copy of orders for fast admin search.

**The code:**
```text
Vue frontend  →  HTTP  →  Spring Boot backend  →  MongoDB      (products)
                                             →  PostgreSQL   (orders: source of truth)
                                             →  Elasticsearch (orders copy: search)
```

**What's happening:** The diagram shows the full data flow. The browser talks only to the Spring Boot backend. The backend talks to three databases. Each request uses only the database that owns that job. The product list reads MongoDB. Order placement writes PostgreSQL. Admin search reads Elasticsearch.

**Why it matters:** One database cannot do all three jobs well. Document search is slow in PostgreSQL. Transactions are weak in MongoDB. Separation keeps each job fast and simple. If the search copy breaks, orders still work.

---
