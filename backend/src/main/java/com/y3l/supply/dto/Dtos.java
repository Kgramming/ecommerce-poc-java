package com.y3l.supply.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** JSON shapes are frozen by CONTRACT.md — field names must stay snake_case on the wire. */
public final class Dtos {
    private Dtos() {}

    public record ProductDto(
            String id, String sku, String title, String description, double price,
            String category, List<String> tags, Map<String, Object> attributes,
            List<Map<String, Object>> variants, boolean active,
            @JsonProperty("updated_at") String updatedAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductCreate(
            String sku, String title, String description, Double price, String category,
            List<String> tags, Map<String, Object> attributes,
            List<Map<String, Object>> variants, Boolean active) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductUpdate(
            String sku, String title, String description, Double price, String category,
            List<String> tags, Map<String, Object> attributes,
            List<Map<String, Object>> variants, Boolean active) {
        public boolean isEmpty() {
            return sku == null && title == null && description == null && price == null
                    && category == null && tags == null && attributes == null
                    && variants == null && active == null;
        }
    }

    public record ProductList(List<ProductDto> products, int total) {}

    public record OrderItemIn(
            @JsonProperty("product_id") String productId,
            @JsonProperty("quantity") int quantity) {}

    public record OrderPlace(
            @JsonProperty("user_id") int userId,
            @JsonProperty("items") List<OrderItemIn> items) {}

    public record OrderItemOut(
            @JsonProperty("product_id") String productId,
            @JsonProperty("title") String title,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unit_price") double unitPrice) {}

    public record CustomerOut(
            @JsonProperty("id") int id,
            @JsonProperty("name") String name,
            @JsonProperty("email") String email) {}

    public record OrderOut(
            @JsonProperty("id") int id,
            @JsonProperty("user_id") int userId,
            @JsonProperty("customer") CustomerOut customer,
            @JsonProperty("order_date") String orderDate,
            @JsonProperty("status") String status,
            @JsonProperty("total_amount") double totalAmount,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("items") List<OrderItemOut> items) {}

    public record StatusUpdate(@JsonProperty("status") String status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchRequest(
            @JsonProperty("query") String query,
            @JsonProperty("statuses") List<String> statuses,
            @JsonProperty("date_from") String dateFrom,
            @JsonProperty("date_to") String dateTo,
            @JsonProperty("min_total") Double minTotal,
            @JsonProperty("max_total") Double maxTotal,
            @JsonProperty("page") Integer page,
            @JsonProperty("page_size") Integer pageSize) {
        public int pageOrDefault() { return page != null && page >= 1 ? page : 1; }
        public int pageSizeOrDefault() {
            int ps = pageSize != null ? pageSize : 20;
            return Math.min(Math.max(ps, 1), 1000);
        }
    }

    public record SearchOrderHit(
            @JsonProperty("order_id") int orderId,
            @JsonProperty("order_date") String orderDate,
            @JsonProperty("status") String status,
            @JsonProperty("total_amount") double totalAmount,
            @JsonProperty("customer") CustomerOut customer,
            @JsonProperty("items") List<OrderItemOut> items) {}

    public record SearchKpis(
            @JsonProperty("total_revenue") double totalRevenue,
            @JsonProperty("count_by_status") Map<String, Integer> countByStatus) {}

    public record SearchResponse(
            @JsonProperty("total") int total,
            @JsonProperty("orders") List<SearchOrderHit> orders,
            @JsonProperty("kpis") SearchKpis kpis) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(@JsonProperty("detail") String detail) {}
}
