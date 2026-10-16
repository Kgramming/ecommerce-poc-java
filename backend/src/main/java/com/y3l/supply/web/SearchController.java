package com.y3l.supply.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.y3l.supply.dto.Dtos.CustomerOut;
import com.y3l.supply.dto.Dtos.OrderItemOut;
import com.y3l.supply.dto.Dtos.SearchKpis;
import com.y3l.supply.dto.Dtos.SearchOrderHit;
import com.y3l.supply.dto.Dtos.SearchRequest;
import com.y3l.supply.dto.Dtos.SearchResponse;
import com.y3l.supply.service.EsClient;
import com.y3l.supply.service.SyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin search route — Elasticsearch ONLY. */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final EsClient es;
    private final SyncService sync;
    private final ObjectMapper om;
    private final String index;

    public SearchController(EsClient es, SyncService sync, ObjectMapper om,
                            @Value("${app.es.index}") String index) {
        this.es = es;
        this.sync = sync;
        this.om = om;
        this.index = index;
    }

    @PostMapping("/orders")
    public SearchResponse search(@RequestBody SearchRequest req) throws Exception {
        ObjectNode body = sync.buildSearchBody(req.query(), req.statuses(),
                req.dateFrom(), req.dateTo(), req.minTotal(), req.maxTotal());
        int page = req.pageOrDefault();
        int pageSize = req.pageSizeOrDefault();
        String raw = es.search(index, om.writeValueAsString(body),
                (page - 1) * pageSize, pageSize);
        JsonNode resp = om.readTree(raw);

        int total = resp.path("hits").path("total").path("value").asInt(0);
        List<SearchOrderHit> orders = new ArrayList<>();
        for (JsonNode h : resp.path("hits").path("hits")) {
            JsonNode s = h.path("_source");
            JsonNode cust = s.path("customer");
            List<OrderItemOut> items = new ArrayList<>();
            for (JsonNode it : s.path("items")) {
                items.add(new OrderItemOut(
                        it.path("product_id").asText(),
                        it.path("title").asText(),
                        it.path("quantity").asInt(0),
                        it.path("unit_price").asDouble(0.0)));
            }
            orders.add(new SearchOrderHit(
                    s.path("order_id").asInt(0),
                    s.path("order_date").asText(""),
                    s.path("status").asText(""),
                    s.path("total_amount").asDouble(0.0),
                    new CustomerOut(cust.path("id").asInt(0),
                            cust.path("name").asText(""),
                            cust.path("email").asText("")),
                    items));
        }

        Map<String, Integer> byStatus = new HashMap<>();
        for (JsonNode b : resp.path("aggregations").path("by_status").path("buckets")) {
            byStatus.put(b.path("key").asText(), b.path("doc_count").asInt(0));
        }
        double revenue = resp.path("aggregations").path("revenue").path("value").asDouble(0.0);
        return new SearchResponse(total, orders, new SearchKpis(revenue, byStatus));
    }
}
