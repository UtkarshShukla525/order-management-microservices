package com.utkarsh.ordermgmt.product.service;

import com.utkarsh.ordermgmt.product.dto.ProductRequest;
import com.utkarsh.ordermgmt.product.dto.ProductResponse;
import com.utkarsh.ordermgmt.product.exception.InsufficientStockException;
import com.utkarsh.ordermgmt.product.exception.ProductNotFoundException;
import com.utkarsh.ordermgmt.product.model.Product;
import com.utkarsh.ordermgmt.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProductOrThrow(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProductOrThrow(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Takes stock out for an order. Runs in one transaction with a row lock so
     * the "check then decrement" cannot be interleaved by another request.
     */
    @Transactional
    public ProductResponse reserveStock(Long id, int quantity) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(id, product.getStockQuantity(), quantity);
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        return ProductResponse.from(product);
    }

    /** Puts stock back, e.g. when an order is cancelled. */
    @Transactional
    public ProductResponse releaseStock(Long id, int quantity) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        return ProductResponse.from(product);
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
