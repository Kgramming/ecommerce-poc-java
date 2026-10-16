# 05 — One Transaction for Order Plus Items

Source files in this byte: `backend/src/main/java/com/y3l/supply/repo/OrderRepo.java`

---

### Byte 5: One transaction for order plus items

**Builds on:** 3, 4

**In plain terms:** A *transaction* is a group of writes that succeed or fail together. `createOrderTx` inserts the order row and all its item rows inside one transaction. The `@Transactional` annotation starts the transaction. If any insert fails, Spring rolls back everything. There is never an order without items, and never items without an order.

**The code:**
```java
// OrderRepo.java — all-or-nothing write
@Transactional
public int createOrderTx(int userId, List<OrderItemOut> snapshots, double total) {
    Integer orderId = jdbc.queryForObject(
            "INSERT INTO orders (user_id, order_date, status, total_amount) " +
            "VALUES (?, now(), 'PENDING', ?) RETURNING id",
            Integer.class, userId, total);
    for (OrderItemOut s : snapshots) {
        jdbc.update(
                "INSERT INTO order_items (order_id, product_id, title, quantity, unit_price) " +
                "VALUES (?, ?, ?, ?, ?)",
                orderId, s.productId(), s.title(), s.quantity(), s.unitPrice());
    }
    return orderId;
}
```

**What's happening:** The annotation opens a transaction before the method runs. The code inserts the order and reads back the new id with `RETURNING`. It then inserts each snapshot item with that id. Spring commits at the end, or rolls back on any error.

**Why it matters:** Money data must never be half-written. Without the transaction, a crash could leave an order with no items and a wrong total. The transaction makes the write atomic.

---
