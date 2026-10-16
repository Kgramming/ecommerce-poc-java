package com.y3l.supply.repo;

import com.y3l.supply.dto.Dtos.CustomerOut;
import com.y3l.supply.dto.Dtos.OrderItemOut;
import com.y3l.supply.dto.Dtos.OrderOut;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL order repository. Orders are the source of truth: every write
 * goes through a real DB transaction. Reads here hit Postgres ONLY.
 */
@Repository
public class OrderRepo {

    private static final String ORDER_SELECT = """
            SELECT o.id, o.user_id, o.order_date, o.status, o.total_amount,
                   o.updated_at, u.name, u.email,
                   i.product_id, i.title, i.quantity, i.unit_price
            FROM orders o
            JOIN users u ON u.id = o.user_id
            LEFT JOIN order_items i ON i.order_id = o.id""";

    private final JdbcTemplate jdbc;

    public OrderRepo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String iso(Object v) {
        if (v == null) return "";
        OffsetDateTime odt;
        if (v instanceof OffsetDateTime o) {
            odt = o;
        } else if (v instanceof java.sql.Timestamp ts) {
            // queryForList returns java.sql.Timestamp for timestamptz columns
            odt = ts.toInstant().atOffset(ZoneOffset.UTC);
        } else if (v instanceof java.util.Date d) {
            odt = d.toInstant().atOffset(ZoneOffset.UTC);
        } else {
            return v.toString();
        }
        return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** Groups the flat join rows into orders, mirroring Python's _group(). */
    static List<OrderOut> group(List<Map<String, Object>> rows) {
        Map<Integer, Acc> accs = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            int oid = ((Number) r.get("id")).intValue();
            Acc acc = accs.computeIfAbsent(oid, k -> {
                int userId = ((Number) r.get("user_id")).intValue();
                return new Acc(
                        oid, userId,
                        new CustomerOut(userId, (String) r.get("name"), (String) r.get("email")),
                        iso(r.get("order_date")),
                        (String) r.get("status"),
                        ((Number) r.get("total_amount")).doubleValue(),
                        iso(r.get("updated_at")));
            });
            if (r.get("product_id") != null) {
                acc.items().add(new OrderItemOut(
                        (String) r.get("product_id"),
                        (String) r.get("title"),
                        ((Number) r.get("quantity")).intValue(),
                        ((Number) r.get("unit_price")).doubleValue()));
            }
        }
        List<OrderOut> out = new ArrayList<>();
        for (Acc a : accs.values()) {
            out.add(new OrderOut(a.id(), a.userId(), a.customer(), a.orderDate(),
                    a.status(), a.totalAmount(), a.updatedAt(), List.copyOf(a.items())));
        }
        return out;
    }

    private record Acc(int id, int userId, CustomerOut customer, String orderDate,
                       String status, double totalAmount, String updatedAt,
                       List<OrderItemOut> items) {
        Acc(int id, int userId, CustomerOut customer, String orderDate,
            String status, double totalAmount, String updatedAt) {
            this(id, userId, customer, orderDate, status, totalAmount, updatedAt,
                    new ArrayList<>());
        }
    }

    public CustomerOut getUser(int userId) {
        List<CustomerOut> found = jdbc.query(
                "SELECT id, name, email FROM users WHERE id = ?",
                (rs, n) -> new CustomerOut(rs.getInt("id"), rs.getString("name"),
                        rs.getString("email")),
                userId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Insert one order + N items atomically. All-or-nothing. Returns the order id. */
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

    public OrderOut getOrder(int orderId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                ORDER_SELECT + " WHERE o.id = ? ORDER BY i.id", orderId);
        List<OrderOut> grouped = group(rows);
        return grouped.isEmpty() ? null : grouped.get(0);
    }

    public OrderOut updateStatus(int orderId, String status) {
        int n = jdbc.update(
                "UPDATE orders SET status = ?, updated_at = now() WHERE id = ?",
                status, orderId);
        if (n == 0) return null;
        return getOrder(orderId);
    }

    public List<Integer> listOrdersSince(OffsetDateTime since) {
        return jdbc.queryForList(
                "SELECT id FROM orders WHERE updated_at > ? ORDER BY updated_at",
                Integer.class, since);
    }

    public List<OrderOut> listAllOrders() {
        return group(jdbc.queryForList(ORDER_SELECT + " ORDER BY o.id, i.id"));
    }
}
