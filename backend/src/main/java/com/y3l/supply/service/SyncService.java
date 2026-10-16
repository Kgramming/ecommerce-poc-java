package com.y3l.supply.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.y3l.supply.dto.Dtos.OrderOut;
import com.y3l.supply.repo.OrderRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Elasticsearch synchronization — keeps the admin dashboard in line with Postgres.
 * Mirrors the Python reference: dual-write (A) + polling worker (B).
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final EsClient es;
    private final ObjectMapper om;
    private final String index;

    public SyncService(EsClient es, ObjectMapper om,
                       @Value("${app.es.index}") String index) {
        this.es = es;
        this.om = om;
        this.index = index;
    }

    public String indexName() { return index; }

    /** Flatten a Postgres order (+ snapshots) into the ES `orders` document. */
    public ObjectNode buildEsDoc(OrderOut order) {
        ObjectNode doc = om.createObjectNode();
        doc.put("order_id", order.id());
        doc.put("order_date", order.orderDate());
        doc.put("status", order.status());
        doc.put("total_amount", order.totalAmount());
        doc.put("updated_at", order.updatedAt());
        doc.set("customer", om.valueToTree(order.customer()));
        doc.set("items", om.valueToTree(order.items()));
        return doc;
    }

    public void ensureIndex() {
        try {
            if (es.indexExists(index)) return;
            ClassPathResource res = new ClassPathResource("es-mapping.json");
            String raw = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode mappings = om.readTree(raw).get("mappings");
            es.createIndex(index, om.writeValueAsString(mappings));
            log.info("created ES index {}", index);
        } catch (Exception e) {
            // never fail boot because ES is napping
            log.warn("ES index ensure failed at startup: {}", e.getMessage());
        }
    }

    /** wait_for refresh: the doc must be searchable the moment the API returns. */
    public void indexOrder(OrderOut order) throws Exception {
        es.indexDoc(index, String.valueOf(order.id()),
                om.writeValueAsString(buildEsDoc(order)), true);
    }

    public void indexOrdersBulk(List<OrderOut> orders) throws Exception {
        if (orders.isEmpty()) return;
        StringBuilder nd = new StringBuilder();
        for (OrderOut o : orders) {
            ObjectNode action = om.createObjectNode();
            ObjectNode meta = om.createObjectNode();
            meta.put("_index", index);
            meta.put("_id", String.valueOf(o.id()));
            action.set("index", meta);
            nd.append(om.writeValueAsString(action)).append('\n');
            nd.append(om.writeValueAsString(buildEsDoc(o))).append('\n');
        }
        es.bulk(nd.toString());
    }

    public int reindexAll(OrderRepo orderRepo) throws Exception {
        List<OrderOut> all = orderRepo.listAllOrders();
        indexOrdersBulk(all);
        return all.size();
    }

    /** Build the exact search body the Python reference sends. */
    public ObjectNode buildSearchBody(String query, List<String> statuses,
                                      String dateFrom, String dateTo,
                                      Double minTotal, Double maxTotal) {
        ArrayNode must = om.createArrayNode();
        if (query != null && !query.isBlank()) {
            // customer.name is top-level, but items.title lives inside a NESTED
            // field: a plain multi_match on it silently matches nothing.
            ObjectNode mm = om.createObjectNode();
            mm.put("query", query);
            ArrayNode fields = om.createArrayNode().add("customer.name");
            mm.set("fields", fields);
            mm.put("boost", 2.0);
            ObjectNode should1 = om.createObjectNode();
            should1.set("multi_match", mm);

            ObjectNode innerMm = om.createObjectNode();
            innerMm.put("query", query);
            innerMm.set("fields", om.createArrayNode().add("items.title"));
            ObjectNode nestedQ = om.createObjectNode();
            nestedQ.set("multi_match", innerMm);
            ObjectNode nested = om.createObjectNode();
            nested.put("path", "items");
            nested.set("query", nestedQ);
            nested.put("score_mode", "avg");
            ObjectNode should2 = om.createObjectNode();
            should2.set("nested", nested);

            ObjectNode bool = om.createObjectNode();
            bool.set("should", om.createArrayNode().add(should1).add(should2));
            bool.put("minimum_should_match", 1);
            ObjectNode clause = om.createObjectNode();
            clause.set("bool", bool);
            must.add(clause);
        }

        ArrayNode filters = om.createArrayNode();
        if (statuses != null && !statuses.isEmpty()) {
            ObjectNode terms = om.createObjectNode();
            ArrayNode vals = om.createArrayNode();
            statuses.forEach(vals::add);
            terms.set("status", vals);
            ObjectNode f = om.createObjectNode();
            f.set("terms", terms);
            filters.add(f);
        }
        if (dateFrom != null || dateTo != null) {
            ObjectNode r = om.createObjectNode();
            if (dateFrom != null) r.put("gte", dateFrom);
            if (dateTo != null) r.put("lte", dateTo);
            ObjectNode range = om.createObjectNode();
            range.set("order_date", r);
            ObjectNode f = om.createObjectNode();
            f.set("range", range);
            filters.add(f);
        }
        if (minTotal != null || maxTotal != null) {
            ObjectNode r = om.createObjectNode();
            if (minTotal != null) r.put("gte", minTotal);
            if (maxTotal != null) r.put("lte", maxTotal);
            ObjectNode range = om.createObjectNode();
            range.set("total_amount", r);
            ObjectNode f = om.createObjectNode();
            f.set("range", range);
            filters.add(f);
        }

        ObjectNode bool = om.createObjectNode();
        if (must.isEmpty()) {
            ObjectNode matchAll = om.createObjectNode();
            matchAll.set("match_all", om.createObjectNode());
            must.add(matchAll);
        }
        bool.set("must", must);
        bool.set("filter", filters);

        ObjectNode body = om.createObjectNode();
        ObjectNode q = om.createObjectNode();
        q.set("bool", bool);
        body.set("query", q);

        ObjectNode aggs = om.createObjectNode();
        ObjectNode byStatus = om.createObjectNode();
        ObjectNode terms = om.createObjectNode();
        terms.put("field", "status");
        byStatus.set("terms", terms);
        aggs.set("by_status", byStatus);
        ObjectNode revenue = om.createObjectNode();
        ObjectNode sum = om.createObjectNode();
        sum.put("field", "total_amount");
        revenue.set("sum", sum);
        aggs.set("revenue", revenue);
        body.set("aggs", aggs);

        ObjectNode sortField = om.createObjectNode();
        sortField.put("order", "desc");
        ObjectNode sortEntry = om.createObjectNode();
        sortEntry.set("order_date", sortField);
        body.set("sort", om.createArrayNode().add(sortEntry));
        return body;
    }
}
