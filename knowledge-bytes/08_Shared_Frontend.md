# 08 — One UI for Every Language Port

Source files in this byte: `frontend/src/`, `frontend/vite.config.js`, `CONTRACT.md`

---

### Byte 8: One UI for every language port

**Builds on:** 1

**In plain terms:** The frontend is a Vue 3 single-page app with five screens: storefront, checkout, admin search, order detail, and catalog. It talks to the backend only through `/api`, which the Vite dev server proxies to the backend port. The whole `frontend/` folder is copied unchanged into every language port. UI work pays off four times.

**The code:**
```js
// frontend/vite.config.js — the proxy keeps the UI backend-agnostic
proxy: {
  '/api': {
    target: process.env.VITE_API_URL || 'http://localhost:8000',
    changeOrigin: true,
  },
},
```

**What's happening:** The Vue code uses relative URLs like `/api/products`. The dev server forwards those to the backend. The `VITE_API_URL` variable selects the backend: port 8000 for Python, 8001 for Java. The UI code never names a language or a port.

**Why it matters:** The API contract is frozen in `CONTRACT.md`. Because the UI only depends on the contract, one UI serves all four backends. A UI fix in one repo applies to every port.

---

## PUTTING IT TOGETHER

Three databases divide the work: MongoDB serves products, PostgreSQL owns orders, Elasticsearch serves search. Order placement validates against the live catalog, freezes a snapshot of title and price, and writes order plus items in one Postgres transaction via `@Transactional`. Dual-write pushes the order to Elasticsearch at once, and the `PollWorker` thread repairs any gap. The admin search reads only Elasticsearch, with a nested query for item titles. The Vue UI talks to any backend through the frozen contract. Each byte is one part of the puzzle. Together they are the full system.
