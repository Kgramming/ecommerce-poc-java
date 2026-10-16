package com.y3l.supply.web;

import com.y3l.supply.dto.Dtos.OrderOut;
import com.y3l.supply.dto.Dtos.OrderPlace;
import com.y3l.supply.dto.Dtos.StatusUpdate;
import com.y3l.supply.repo.OrderRepo;
import com.y3l.supply.service.OrderService;
import com.y3l.supply.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/** Order routes — Postgres is the source of truth. */
@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);
    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "PROCESSING", "SHIPPED");

    private final OrderService orderService;
    private final OrderRepo repo;
    private final SyncService sync;

    public OrdersController(OrderService orderService, OrderRepo repo, SyncService sync) {
        this.orderService = orderService;
        this.repo = repo;
        this.sync = sync;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderOut create(@RequestBody OrderPlace payload) {
        return orderService.placeOrder(payload.userId(), payload.items());
    }

    @GetMapping("/{oid}")
    public OrderOut get(@PathVariable int oid) {
        OrderOut o = repo.getOrder(oid);
        if (o == null) throw ApiException.notFound("order not found");
        return o;
    }

    @PatchMapping("/{oid}/status")
    public OrderOut patchStatus(@PathVariable int oid, @RequestBody StatusUpdate payload) {
        if (payload.status() == null || !VALID_STATUSES.contains(payload.status())) {
            throw ApiException.unprocessable(
                    "status must be one of " + VALID_STATUSES.stream().sorted().toList());
        }
        OrderOut o = repo.updateStatus(oid, payload.status());
        if (o == null) throw ApiException.notFound("order not found");
        // Keep the dashboard accurate: refresh the ES doc. If ES is down the
        // write path still succeeded — the polling worker recovers it.
        try {
            sync.indexOrder(o);
        } catch (Exception e) {
            log.warn("ES status-sync failed for order {}: {}", oid, e.getMessage());
        }
        return o;
    }
}
