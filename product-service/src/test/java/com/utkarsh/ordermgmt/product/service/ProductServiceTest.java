package com.utkarsh.ordermgmt.product.service;

import com.utkarsh.ordermgmt.product.dto.ProductRequest;
import com.utkarsh.ordermgmt.product.dto.ProductResponse;
import com.utkarsh.ordermgmt.product.exception.InsufficientStockException;
import com.utkarsh.ordermgmt.product.exception.ProductNotFoundException;
import com.utkarsh.ordermgmt.product.model.Product;
import com.utkarsh.ordermgmt.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct(int stock) {
        Product product = new Product("Laptop", "14-inch laptop", new BigDecimal("999.99"), stock);
        product.setId(1L);
        return product;
    }

    @Test
    void create_savesProductAndReturnsResponse() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductResponse response = productService.create(
                new ProductRequest("Laptop", "14-inch laptop", new BigDecimal("999.99"), 10));

        assertEquals(1L, response.id());
        assertEquals("Laptop", response.name());
        assertEquals(10, response.stockQuantity());
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.findById(99L));
    }

    @Test
    void reserveStock_reducesStock() {
        Product product = sampleProduct(10);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.reserveStock(1L, 3);

        assertEquals(7, response.stockQuantity());
        assertEquals(7, product.getStockQuantity());
    }

    @Test
    void reserveStock_whenNotEnoughStock_throwsAndLeavesStockUnchanged() {
        Product product = sampleProduct(2);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> productService.reserveStock(1L, 5));
        assertEquals(2, product.getStockQuantity());
    }

    @Test
    void releaseStock_addsStockBack() {
        Product product = sampleProduct(5);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.releaseStock(1L, 3);

        assertEquals(8, response.stockQuantity());
    }
}
