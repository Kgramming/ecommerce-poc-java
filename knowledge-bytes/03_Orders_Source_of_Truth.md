# 03 — Orders Are the Source of Truth in PostgreSQL

Source files in this byte: `backend/src/main/java/com/y3l/supply/repo/OrderRepo.java`, `seed/schema.sql`

---

### Byte 3: Orders are the source of truth in PostgreSQL

**Builds on:** 1

**In plain terms:** An order has a buyer, a date, a status, a total, and line items. PostgreSQL stores this in three tables: `users`, `orders`, and `order_items`. The API joins the three tables and groups the rows back into one order object with an `items` list.

**The code:**
```java
// OrderRepo.java — the one join that builds an order
private static final String ORDER_SELECT = """
        SELECT o.id, o.user_id, o.order_date, o.status, o.total_amount,
               o.updated_at, u.name, u.email,
               i.product_id, i.title, i.quantity, i.unit_price
        FROM orders o
        JOIN users u ON u.id = o.user_id
        LEFT JOIN order_items i ON i.order_id = o.id""";
```

**What's happening:** The query joins `orders` to `users` for the buyer name and email. It left-joins `order_items` for the line items. One order with three items returns three rows. The `group` method folds those rows into one `OrderOut` with an `items` list.

**Why it matters:** Fulfillment reads must never touch the search copy. The search copy can lag or break. This query reads the truth directly. Every order response in the API comes from this join.

---
