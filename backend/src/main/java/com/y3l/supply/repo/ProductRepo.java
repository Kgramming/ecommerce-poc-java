package com.y3l.supply.repo;

import com.y3l.supply.dto.Dtos.ProductCreate;
import com.y3l.supply.dto.Dtos.ProductDto;
import com.y3l.supply.dto.Dtos.ProductUpdate;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** MongoDB product-catalog repository. Products are read from MongoDB ONLY. */
@Repository
public class ProductRepo {

    private final MongoTemplate mongo;

    public ProductRepo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private static String utcNow() {
        return OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .replace("+00:00", "Z");
    }

    @SuppressWarnings("unchecked")
    static ProductDto toProduct(Document doc) {
        return new ProductDto(
                doc.getObjectId("_id").toHexString(),
                doc.getString("sku"),
                doc.getString("title"),
                doc.getString("description") != null ? doc.getString("description") : "",
                ((Number) doc.get("price")).doubleValue(),
                doc.getString("category"),
                doc.get("tags") != null ? (List<String>) doc.get("tags") : List.of(),
                doc.get("attributes") != null ? (Map<String, Object>) doc.get("attributes") : Map.of(),
                doc.get("variants") != null ? (List<Map<String, Object>>) doc.get("variants") : List.of(),
                doc.getBoolean("active", true),
                doc.getString("updated_at") != null ? doc.getString("updated_at") : "");
    }

    public List<ProductDto> list(String category, String q, boolean includeInactive) {
        List<Criteria> ands = new ArrayList<>();
        if (!includeInactive) {
            ands.add(Criteria.where("active").is(true));
        }
        if (category != null && !category.isBlank()) {
            // case-insensitive exact match: the UI sends "Audio", the DB stores "audio"
            ands.add(Criteria.where("category")
                    .regex("^" + Pattern.quote(category) + "$", "i"));
        }
        if (q != null && !q.isBlank()) {
            ands.add(new Criteria().orOperator(
                    Criteria.where("title").regex(q, "i"),
                    Criteria.where("tags").regex(q, "i")));
        }
        Query query = ands.isEmpty() ? new Query() : new Query(new Criteria().andOperator(ands));
        query.with(Sort.by(Sort.Direction.ASC, "title"));
        return mongo.find(query, Document.class, "products").stream()
                .map(ProductRepo::toProduct).collect(Collectors.toList());
    }

    public ProductDto get(String pid) {
        ObjectId oid;
        try {
            oid = new ObjectId(pid);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Document doc = mongo.findById(oid, Document.class, "products");
        return doc != null ? toProduct(doc) : null;
    }

    public Map<String, ProductDto> getMany(List<String> pids) {
        List<ObjectId> oids = pids.stream()
                .map(pid -> {
                    try { return new ObjectId(pid); }
                    catch (IllegalArgumentException e) { return null; }
                })
                .filter(o -> o != null)
                .collect(Collectors.toList());
        if (oids.isEmpty()) return Map.of();
        Map<String, ProductDto> out = new HashMap<>();
        for (Document d : mongo.find(Query.query(Criteria.where("_id").in(oids)),
                Document.class, "products")) {
            out.put(d.getObjectId("_id").toHexString(), toProduct(d));
        }
        return out;
    }

    public ProductDto create(ProductCreate data) {
        Document doc = new Document()
                .append("sku", data.sku())
                .append("title", data.title())
                .append("description", data.description() != null ? data.description() : "")
                .append("price", data.price())
                .append("category", data.category())
                .append("tags", data.tags() != null ? data.tags() : List.of())
                .append("attributes", data.attributes() != null ? data.attributes() : Map.of())
                .append("variants", data.variants() != null ? data.variants() : List.of())
                .append("active", data.active() != null ? data.active() : true)
                .append("updated_at", utcNow());
        mongo.insert(doc, "products");
        return toProduct(doc);
    }

    public ProductDto update(String pid, ProductUpdate patch) {
        ObjectId oid;
        try {
            oid = new ObjectId(pid);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Update update = new Update().set("updated_at", utcNow());
        if (patch.sku() != null) update.set("sku", patch.sku());
        if (patch.title() != null) update.set("title", patch.title());
        if (patch.description() != null) update.set("description", patch.description());
        if (patch.price() != null) update.set("price", patch.price());
        if (patch.category() != null) update.set("category", patch.category());
        if (patch.tags() != null) update.set("tags", patch.tags());
        if (patch.attributes() != null) update.set("attributes", patch.attributes());
        if (patch.variants() != null) update.set("variants", patch.variants());
        if (patch.active() != null) update.set("active", patch.active());
        Document doc = mongo.findAndModify(Query.query(Criteria.where("_id").is(oid)),
                update,
                new org.springframework.data.mongodb.core.FindAndModifyOptions().returnNew(true),
                Document.class, "products");
        return doc != null ? toProduct(doc) : null;
    }
}
