package com.y3l.supply.service;

import com.y3l.supply.dto.Dtos.CustomerOut;
import com.y3l.supply.dto.Dtos.OrderItemIn;
import com.y3l.supply.dto.Dtos.OrderItemOut;
import com.y3l.supply.dto.Dtos.OrderOut;
import com.y3l.supply.dto.Dtos.ProductDto;
import com.y3l.supply.repo.OrderRepo;
import com.y3l.supply.repo.ProductRepo;
import com.y3l.supply.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Order placement orchestration — the transactional write path.
 * 1. Validate the user exists (Postgres).
 * 2. Validate every line item against the LIVE MongoDB catalog (active products).
 * 3. Snapshot title + unit price at order time.
 * 4. Insert order + items inside ONE Postgres transaction (all-or-nothing).
 * 5. Fire the ES sync trigger (dual-write now; the polling worker also picks it up).
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepo orders;
    private final ProductRepo products;
    private final SyncService sync;
    private final String syncStrategy;

    public OrderService(OrderRepo orders, ProductRepo products, SyncService sync,
                        @Value("${app.sync.strategy}") String syncStrategy) {
        this.orders = orders;
        this.products = products;
        this.sync = sync;
        this.syncStrategy = syncStrategy;
    }

    public OrderOut placeOrder(int userId, List<OrderItemIn> items) {
        CustomerOut user = orders.getUser(userId);
        if (user == null) {
            throw ApiException.badRequest("unknown user_id " + userId);
        }
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("items must not be empty");
        }
        for (OrderItemIn it : items) {
            if (it.quantity() <= 0) {
                throw ApiException.badRequest("quantity must be > 0");
            }
        }

        Map<String, Integer> wanted = new LinkedHashMap<>();
        for (OrderItemIn it : items) wanted.put(it.productId(), it.quantity());
        Map<String, ProductDto> found = products.getMany(new ArrayList<>(wanted.keySet()));

        List<OrderItemOut> snapshots = new ArrayList<>();
        for (Map.Entry<String, Integer> e : wanted.entrySet()) {
            ProductDto p = found.get(e.getKey());
            if (p == null) {
                throw ApiException.badRequest("unknown product_id " + e.getKey());
            }
            if (!p.active()) {
                throw ApiException.badRequest("product '" + p.title() + "' is not active");
            }
            snapshots.add(new OrderItemOut(e.getKey(), p.title(), e.getValue(), p.price()));
        }

        double total = Math.round(
                snapshots.stream().mapToDouble(s -> s.unitPrice() * s.quantity()).sum() * 100.0) / 100.0;
        int orderId = orders.createOrderTx(userId, snapshots, total);
        OrderOut order = orders.getOrder(orderId);

        if ("dual_write".equals(syncStrategy)) {
            try {
                sync.indexOrder(order);
            } catch (Exception ex) {
                // Write path already committed: never fail the order because ES
                // is napping. Logged; polling worker or manual reindex recovers.
                log.error("dual-write to ES failed for order {}: {}", orderId, ex.getMessage());
            }
        }
        return order;
    }
}
