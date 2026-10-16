# 02 — The Product Catalog Lives in MongoDB

Source files in this byte: `backend/src/main/java/com/y3l/supply/repo/ProductRepo.java`

---

### Byte 2: The product catalog lives in MongoDB

**Builds on:** 1

**In plain terms:** Products are documents. A document is a JSON-like record with flexible fields. MongoDB stores documents. Each product has a SKU, a title, a price, a category, tags, and an `active` flag. The API exposes the MongoDB `_id` as a plain string field named `id`.

**The code:**
```java
// ProductRepo.java — every product read goes through here
static ProductDto toProduct(Document doc) {
    return new ProductDto(
            doc.getObjectId("_id").toHexString(),  // ObjectId -> hex string
            doc.getString("sku"),
            doc.getString("title"),
            ((Number) doc.get("price")).doubleValue(),
            doc.getString("category"),
            ...
            doc.getBoolean("active", true),
            ...);
}
```

**What's happening:** `toProduct` converts a raw MongoDB document into the API shape. It turns the `_id` ObjectId into a hex string. It forces `price` to a double. It defaults `active` to true when the field is absent. Every product endpoint returns this shape.

**Why it matters:** The frontend and the order code both expect string IDs. Without this conversion, the API would leak database internals. One function guarantees one shape everywhere.

---
