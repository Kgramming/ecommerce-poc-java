# Frontend — Meridian Supply Co.

Vue 3 + Vite single-page app. **The same SPA is copied unchanged into every
language repository** (Python, Java, .NET 10, Node.js) — the backend API
contract is identical across all four.

## Run (dev)

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

The dev server proxies `/api/*` to `http://localhost:8000` (the backend), so
no CORS configuration is needed. Override the target with `VITE_API_URL`.

```bash
VITE_API_URL=http://localhost:8001 npm run dev
```

## Build

```bash
npm run build      # outputs to frontend/dist/
```

## Screens

| Route              | Screen                  | Data source              |
|--------------------|-------------------------|--------------------------|
| `/`                | Storefront (Screen 1)   | MongoDB via `/api/products` |
| `/checkout`        | Checkout (Screen 2)     | `POST /api/orders`       |
| `/admin`           | Order search (Screen 3) | Elasticsearch via `/api/search/orders` |
| `/admin/orders/:id`| Order detail + status   | PostgreSQL (+ ES mirror on update) |
| `/admin/catalog`   | Catalog admin (Screen 4)| MongoDB via `/api/products` |

## Design notes

- One warm paper, one ink, one persimmon accent. No gradients, no emoji icons.
- Fraunces (display serif) for headings, Inter for UI, IBM Plex Mono for
  numbers — tabular figures for all money/IDs.
- Checkout places orders as demo user Jane Smith (seed user #2).
- Cart persists to `localStorage`.
