package com.utkarsh.ordermgmt.product.controller;

import com.utkarsh.ordermgmt.product.dto.ProductRequest;
import com.utkarsh.ordermgmt.product.dto.ProductResponse;
import com.utkarsh.ordermgmt.product.dto.StockRequest;
import com.utkarsh.ordermgmt.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Called by order-service when an order is placed. */
    @PostMapping("/{id}/reserve")
    public ProductResponse reserve(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return productService.reserveStock(id, request.quantity());
    }

    /** Called by order-service when an order is cancelled (or fails to save). */
    @PostMapping("/{id}/release")
    public ProductResponse release(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return productService.releaseStock(id, request.quantity());
    }
}
