# 07 — The Nested Query Trap

Source files in this byte: `backend/src/main/java/com/y3l/supply/service/SyncService.java`, `backend/src/main/resources/es-mapping.json`

---

### Byte 7: The nested query trap

**Builds on:** 6

**In plain terms:** In the Elasticsearch mapping, `items` is a *nested* field. A nested field stores each array element as a separate hidden document. A plain `multi_match` on `items.title` silently matches nothing. The query must wrap the match in a `nested` block that names the path. This bug cost a full debug cycle in the Python port: the search returned zero rows with no error.

**The code:**
```java
// SyncService.java — buildSearchBody, the fix for the silent empty result
ObjectNode nested = om.createObjectNode();
nested.put("path", "items");
nested.set("query", nestedQ);          // multi_match on items.title
nested.put("score_mode", "avg");
ObjectNode should2 = om.createObjectNode();
should2.set("nested", nested);
```

**What's happening:** `buildSearchBody` builds two clauses for a text query. Customer names use a plain `multi_match` with a 2x boost. Item titles use the `nested` wrapper above. The wrapper tells Elasticsearch to search inside the hidden item documents and average their scores.

**Why it matters:** Silent wrong results are worse than errors. The admin dashboard is the main demo screen. Without this wrapper, product-name search shows nothing and the demo fails.

---
