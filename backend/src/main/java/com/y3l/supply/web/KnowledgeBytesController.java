package com.y3l.supply.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knowledge Bytes API — serves the repo's knowledge-bytes/*.md as JSON.
 *
 * GET /api/knowledge-bytes       -> {"bytes": [{"id": "...", "title": "..."}], "total": 8}
 * GET /api/knowledge-bytes/{id}  -> {"id": "...", "title": "...", "markdown": "..."} | 404
 */
@RestController
@RequestMapping("/api/knowledge-bytes")
public class KnowledgeBytesController {

    private static final Pattern BYTE_FILE = Pattern.compile("^\\d{2}_.*\\.md$");
    private static final Pattern SLUG = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern TITLE = Pattern.compile("^#\\s+\\d+\\s+[—–-]\\s+(.+)$", Pattern.MULTILINE);

    private Path resolveDir() {
        String env = System.getenv("KNOWLEDGE_BYTES_DIR");
        if (env != null && !env.isBlank()) return Paths.get(env);
        String cwd = System.getProperty("user.dir");
        for (String cand : new String[]{
                cwd + "/knowledge-bytes",
                cwd + "/../knowledge-bytes",
                cwd + "/../../knowledge-bytes"}) {
            Path p = Paths.get(cand).normalize();
            if (Files.isDirectory(p)) return p;
        }
        return null;
    }

    private String[] parse(Path path) throws IOException {
        String text = Files.readString(path);
        Matcher m = TITLE.matcher(text);
        String title = m.find() ? m.group(1).strip()
                : path.getFileName().toString().replaceAll("\\.md$", "");
        return new String[]{title, text};
    }

    @GetMapping
    public Map<String, Object> list() throws IOException {
        List<Map<String, String>> items = new ArrayList<>();
        Path dir = resolveDir();
        if (dir != null) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.md")) {
                List<Path> files = new ArrayList<>();
                ds.forEach(files::add);
                files.sort(Comparator.comparing(p -> p.getFileName().toString()));
                for (Path f : files) {
                    String fn = f.getFileName().toString();
                    if (!BYTE_FILE.matcher(fn).matches()) continue;
                    String slug = fn.substring(0, fn.length() - 3);
                    String[] parsed = parse(f);
                    items.add(Map.of("id", slug, "title", parsed[0]));
                }
            }
        }
        return Map.of("bytes", items, "total", items.size());
    }

    @GetMapping("/{slug}")
    public Map<String, String> get(@PathVariable String slug) throws IOException {
        if (!SLUG.matcher(slug).matches() || !BYTE_FILE.matcher(slug + ".md").matches()) {
            throw ApiException.notFound("knowledge byte not found");
        }
        Path dir = resolveDir();
        Path path = dir != null ? dir.resolve(slug + ".md") : null;
        if (path == null || !Files.isRegularFile(path)) {
            throw ApiException.notFound("knowledge byte not found");
        }
        String[] parsed = parse(path);
        return Map.of("id", slug, "title", parsed[0], "markdown", parsed[1]);
    }
}
