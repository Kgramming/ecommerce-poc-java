package com.y3l.supply.web;

import com.y3l.supply.dto.Dtos.ProductCreate;
import com.y3l.supply.dto.Dtos.ProductDto;
import com.y3l.supply.dto.Dtos.ProductList;
import com.y3l.supply.dto.Dtos.ProductUpdate;
import com.y3l.supply.repo.ProductRepo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Product catalog routes — MongoDB ONLY. */
@RestController
@RequestMapping("/api/products")
public class ProductsController {

    private final ProductRepo repo;

    public ProductsController(ProductRepo repo) {
        this.repo = repo;
    }

    @GetMapping
    public ProductList list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(name = "include_inactive", defaultValue = "false") boolean includeInactive) {
        List<ProductDto> items = repo.list(category, q, includeInactive);
        return new ProductList(items, items.size());
    }

    @GetMapping("/{pid}")
    public ProductDto get(@PathVariable String pid) {
        ProductDto p = repo.get(pid);
        if (p == null) throw ApiException.notFound("product not found");
        return p;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@RequestBody ProductCreate payload) {
        if (payload.sku() == null || payload.sku().isBlank()
                || payload.title() == null || payload.title().isBlank()
                || payload.category() == null || payload.category().isBlank()) {
            throw ApiException.badRequest("sku, title and category are required");
        }
        if (payload.price() == null || payload.price() <= 0) {
            throw ApiException.badRequest("price must be > 0");
        }
        return repo.create(payload);
    }

    @PutMapping("/{pid}")
    public ProductDto update(@PathVariable String pid, @RequestBody ProductUpdate payload) {
        if (payload == null || payload.isEmpty()) {
            throw ApiException.badRequest("empty update");
        }
        if (payload.price() != null && payload.price() <= 0) {
            throw ApiException.badRequest("price must be > 0");
        }
        ProductDto p = repo.update(pid, payload);
        if (p == null) throw ApiException.notFound("product not found");
        return p;
    }
}
