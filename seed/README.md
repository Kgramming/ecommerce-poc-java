# Seed dataset (shared by all 4 backend ports)

Language-neutral, deterministic seed files. Every backend port loads THESE files —
no port may invent its own seed data.

## Files
- `schema.sql` — PostgreSQL DDL: users, orders, order_items (+ indexes)
- `users.sql` — 8 fixed users (names/emails must match exactly; the "Log In As" dropdown)
- `products.json` — 26 products (25 active + 1 inactive `OG-9999`). `_id` is a 24-char
  hex string: convert to a real ObjectId on insert (pymongo: `ObjectId(...)`, etc.)
- `orders.sql` — 40 orders + 85 line items. `product_id` = MongoDB ObjectId as text;
  `title`/`unit_price` are checkout-time snapshots (pre-rename: "Wireless Mouse" @ 50.16)
- `es-mapping.json` — Elasticsearch `orders` index mapping

## Load order
1. `schema.sql` → 2. `users.sql` → 3. `products.json` → 4. `orders.sql`
5. Reindex ES from Postgres (each backend implements this)
6. **Rename fixture (required, last step):** update the MongoDB `Wireless Mouse` document
   to title `Wireless Mouse Pro`, price `59.99` — WITHOUT touching order_items or ES docs.
   Screen 1/5 show the new title; old orders keep the snapshot. This is the fixture for
   the "Snapshotting" learning milestone.

## Regenerating
`python3 tools/gen_seed.py` — asserts every PDF seed rule (counts, bands, fixtures,
totals) and fails loudly if any rule breaks. Deterministic: no randomness involved.
