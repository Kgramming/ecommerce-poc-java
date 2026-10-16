package com.y3l.supply.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal Elasticsearch HTTP client (java.net.http) — sends the same raw JSON
 * bodies the Python reference builds, so behavior stays identical.
 */
public class EsClient {

    private final HttpClient http;
    private final String baseUrl;

    public EsClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private String request(String method, String path, String body, String contentType)
            throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30));
        if (body != null) {
            b.method(method, HttpRequest.BodyPublishers.ofString(body));
            if (contentType != null) b.header("Content-Type", contentType);
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code < 200 || code >= 300) {
            throw new IOException("ES " + method + " " + path + " -> " + code + ": " + resp.body());
        }
        return resp.body();
    }

    public boolean indexExists(String index) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + index))
                .timeout(Duration.ofSeconds(10))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        int code = http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
        return code == 200;
    }

    public void createIndex(String index, String mappingsJson)
            throws IOException, InterruptedException {
        request("PUT", "/" + index, mappingsJson, "application/json");
    }

    /** Index one doc; refresh=wait_for makes it searchable before the API returns. */
    public void indexDoc(String index, String id, String docJson, boolean refreshWaitFor)
            throws IOException, InterruptedException {
        String path = "/" + index + "/_doc/" + id + (refreshWaitFor ? "?refresh=wait_for" : "");
        request("PUT", path, docJson, "application/json");
    }

    public void bulk(String ndJson) throws IOException, InterruptedException {
        request("POST", "/_bulk", ndJson, "application/x-ndjson");
    }

    public String search(String index, String bodyJson, int from, int size)
            throws IOException, InterruptedException {
        return request("POST", "/" + index + "/_search?from=" + from + "&size=" + size,
                bodyJson, "application/json");
    }
}
