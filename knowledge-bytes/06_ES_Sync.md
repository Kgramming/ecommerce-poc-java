# 06 — Elasticsearch Stays in Sync Two Ways

Source files in this byte: `backend/src/main/java/com/y3l/supply/service/OrderService.java`, `backend/src/main/java/com/y3l/supply/service/PollWorker.java`, `backend/src/main/java/com/y3l/supply/service/SyncService.java`

---

### Byte 6: Elasticsearch stays in sync two ways

**Builds on:** 3, 5

**In plain terms:** Elasticsearch holds a copy of every order for the admin dashboard. Two strategies keep the copy fresh. Strategy A is *dual-write*: after the Postgres transaction commits, the backend indexes the order at once. Strategy B is *polling*: the `PollWorker` thread re-syncs recently changed orders every 15 seconds. If strategy A fails, strategy B repairs the copy.

**The code:**
```java
// OrderService.java — the immediate write, with a safe failure path
if ("dual_write".equals(syncStrategy)) {
    try {
        sync.indexOrder(order);
    } catch (Exception ex) {
        // Write path already committed: never fail the order because ES
        // is napping. Logged; polling worker or manual reindex recovers.
        log.error("dual-write to ES failed for order {}: {}", orderId, ex.getMessage());
    }
}
```

**What's happening:** After `placeOrder` commits, it tries to index the order in Elasticsearch with `refresh="wait_for"`. That flag makes the document searchable before the API returns. If Elasticsearch is down, the code logs the error and returns the order anyway. The sale never fails because search is napping.

**Why it matters:** The dashboard must show new orders at once, but the sale is more important than the dashboard. Dual-write gives speed. The polling worker gives safety. Together they meet the spec: implement at least two sync strategies.

---
