package com.y3l.supply.service;

import com.y3l.supply.dto.Dtos.OrderOut;
import com.y3l.supply.repo.OrderRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Strategy B: periodically sync Postgres -> ES. Only runs when
 * app.sync.strategy=polling (default is dual_write, like the Python reference).
 */
@Component
public class PollWorker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PollWorker.class);

    private final OrderRepo orders;
    private final SyncService sync;
    private final String strategy;
    private final int intervalSeconds;

    public PollWorker(OrderRepo orders, SyncService sync,
                      @Value("${app.sync.strategy}") String strategy,
                      @Value("${app.poll.interval-seconds}") int intervalSeconds) {
        this.orders = orders;
        this.sync = sync;
        this.strategy = strategy;
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"polling".equals(strategy)) return;
        Thread t = new Thread(this::loop, "es-poll-worker");
        t.setDaemon(true);
        t.start();
        log.info("polling sync worker started (every {}s)", intervalSeconds);
    }

    private void loop() {
        OffsetDateTime watermark = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(60);
        try { // full reindex on worker start so nothing is ever missed
            int n = sync.reindexAll(orders);
            log.info("poll worker initial reindex: {} orders", n);
        } catch (Exception e) {
            log.warn("poll worker initial reindex failed: {}", e.getMessage());
        }
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // overlap: idempotent, so nothing is missed
                List<Integer> ids = orders.listOrdersSince(watermark.minusSeconds(30));
                if (!ids.isEmpty()) {
                    List<OrderOut> fresh = new ArrayList<>();
                    for (int id : ids) {
                        OrderOut o = orders.getOrder(id);
                        if (o != null) fresh.add(o);
                    }
                    sync.indexOrdersBulk(fresh);
                    log.info("poll worker synced {} orders", fresh.size());
                }
                watermark = OffsetDateTime.now(ZoneOffset.UTC);
            } catch (Exception e) {
                log.warn("poll worker iteration failed: {}", e.getMessage());
            }
            try {
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
