package com.y3l.supply.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Seed the whole data stack (idempotent: clears and reloads everything).
 * Order: schema -> users -> products (MongoDB) -> orders -> ES reindex
 *        -> rename fixture (Wireless Mouse -> Wireless Mouse Pro, MongoDB only).
 *
 * Run from the repo root:
 *   mvn -q -f backend/pom.xml compile exec:java \
 *     -Dexec.mainClass="com.y3l.supply.seed.SeedRunner"
 */
public class SeedRunner {

    private static final ObjectMapper OM = new ObjectMapper();

    static String env(String k, String def) {
        String v = System.getenv(k);
        return v != null && !v.isBlank() ? v : def;
    }

    static final String PG_URL = "jdbc:postgresql://"
            + env("PGHOST", "localhost") + ":" + env("PGPORT", "5435") + "/"
            + env("PGDATABASE", "ecommerce");
    static final String PG_USER = env("PGUSER", "ecommerce");
    static final String PG_PASS = env("PGPASSWORD", "ecommerce");
    static final String MONGO_URI = "mongodb://"
            + env("MONGOHOST", "localhost") + ":" + env("MONGOPORT", "27017") + "/"
            + env("MONGODB", "ecommerce");
    static final String ES_URL = "http://" + env("ESHOST", "localhost") + ":"
            + env("ESPORT", "9200");
    static final String ES_INDEX = env("ESINDEX", "orders");

    public static void main(String[] args) throws Exception {
        Path seedDir = Path.of("seed");
        if (!Files.isDirectory(seedDir)) {
            throw new IllegalStateException("run from the repo root (seed/ not found)");
        }

        System.out.println("1/6  PostgreSQL schema ...");
        runSql(seedDir.resolve("schema.sql"));
        System.out.println("     clearing any previous seed data ...");
        try (Connection c = pg(); Statement st = c.createStatement()) {
            st.execute("TRUNCATE TABLE order_items, orders, users RESTART IDENTITY CASCADE;");
        }

        System.out.println("2/6  users ...");
        runSql(seedDir.resolve("users.sql"));

        System.out.println("3/6  products -> MongoDB ...");
        String productsJson = Files.readString(seedDir.resolve("products.json"));
        List<Document> products = new ArrayList<>();
        for (JsonNode p : OM.readTree(productsJson)) {
            Document d = Document.parse(p.toString());
            d.put("_id", new ObjectId(d.getString("_id")));
            products.add(d);
        }
        try (MongoClient mongo = MongoClients.create(MONGO_URI)) {
            MongoCollection<Document> col = mongo.getDatabase(env("MONGODB", "ecommerce"))
                    .getCollection("products");
            col.deleteMany(new Document());
            col.insertMany(products);
            System.out.println("     inserted " + products.size() + " products");

            System.out.println("4/6  orders -> PostgreSQL ...");
            runSql(seedDir.resolve("orders.sql"));

            System.out.println("5/6  Elasticsearch reindex ...");
            int n = reindexEs();
            System.out.println("     indexed " + n + " orders");

            System.out.println("6/6  rename fixture: 'Wireless Mouse' -> 'Wireless Mouse Pro' ...");
            String now = OffsetDateTime.now(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+00:00", "Z");
            long matched = col.updateOne(Filters.eq("sku", "WM-001"),
                    Updates.combine(Updates.set("title", "Wireless Mouse Pro"),
                            Updates.set("price", 4999),
                            Updates.set("updated_at", now))).getMatchedCount();
            System.out.println("     matched " + matched + " product "
                    + "(historical orders keep the old snapshot)");
        }
        System.out.println("seed complete.");
    }

    static Connection pg() throws Exception {
        Connection c = DriverManager.getConnection(PG_URL, PG_USER, PG_PASS);
        c.setAutoCommit(true);
        return c;
    }

    static void runSql(Path path) throws Exception {
        String sql = Files.readString(path, StandardCharsets.UTF_8);
        try (Connection c = pg(); Statement st = c.createStatement()) {
            st.execute(sql); // PG JDBC accepts multi-statement scripts
        }
    }

    /** Read every order from Postgres and bulk-index into ES. Returns the count. */
    static int reindexEs() throws Exception {
        String q = """
                SELECT o.id, o.user_id, o.order_date, o.status, o.total_amount,
                       o.updated_at, u.name, u.email,
                       i.product_id, i.title, i.quantity, i.unit_price
                FROM orders o
                JOIN users u ON u.id = o.user_id
                LEFT JOIN order_items i ON i.order_id = o.id
                ORDER BY o.id, i.id""";
        List<ObjectNode> docs = new ArrayList<>();
        try (Connection c = pg();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(q)) {
            ObjectNode cur = null;
            int curId = -1;
            while (rs.next()) {
                int oid = rs.getInt("id");
                if (oid != curId) {
                    cur = OM.createObjectNode();
                    cur.put("order_id", oid);
                    cur.put("order_date", rs.getObject("order_date", OffsetDateTime.class)
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    cur.put("status", rs.getString("status"));
                    cur.put("total_amount", rs.getDouble("total_amount"));
                    cur.put("updated_at", rs.getObject("updated_at", OffsetDateTime.class)
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    ObjectNode cust = OM.createObjectNode();
                    cust.put("id", rs.getInt("user_id"));
                    cust.put("name", rs.getString("name"));
                    cust.put("email", rs.getString("email"));
                    cur.set("customer", cust);
                    cur.set("items", OM.createArrayNode());
                    docs.add(cur);
                    curId = oid;
                }
                if (rs.getString("product_id") != null) {
                    ObjectNode it = OM.createObjectNode();
                    it.put("product_id", rs.getString("product_id"));
                    it.put("title", rs.getString("title"));
                    it.put("quantity", rs.getInt("quantity"));
                    it.put("unit_price", rs.getDouble("unit_price"));
                    ((ArrayNode) cur.get("items")).add(it);
                }
            }
        }

        HttpClient http = HttpClient.newHttpClient();
        // ensure index
        HttpRequest head = HttpRequest.newBuilder(URI.create(ES_URL + "/" + ES_INDEX))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        if (http.send(head, HttpResponse.BodyHandlers.discarding()).statusCode() == 404) {
            String mapping = OM.readTree(Files.readString(Path.of("seed/es-mapping.json")))
                    .get("mappings").toString();
            HttpRequest put = HttpRequest.newBuilder(URI.create(ES_URL + "/" + ES_INDEX))
                    .PUT(HttpRequest.BodyPublishers.ofString(mapping))
                    .header("Content-Type", "application/json").build();
            http.send(put, HttpResponse.BodyHandlers.ofString());
        }
        if (docs.isEmpty()) return 0;
        StringBuilder nd = new StringBuilder();
        for (ObjectNode d : docs) {
            ObjectNode meta = OM.createObjectNode();
            meta.put("_index", ES_INDEX);
            meta.put("_id", String.valueOf(d.get("order_id").asInt()));
            ObjectNode action = OM.createObjectNode();
            action.set("index", meta);
            nd.append(action).append('\n').append(d).append('\n');
        }
        HttpRequest bulk = HttpRequest.newBuilder(URI.create(ES_URL + "/_bulk"))
                .POST(HttpRequest.BodyPublishers.ofString(nd.toString()))
                .header("Content-Type", "application/x-ndjson").build();
        HttpResponse<String> resp = http.send(bulk, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("ES bulk failed: " + resp.body());
        }
        return docs.size();
    }
}
