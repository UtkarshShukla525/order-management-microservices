package com.utkarsh.ordermgmt.product.config;

import com.utkarsh.ordermgmt.product.model.Product;
import com.utkarsh.ordermgmt.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Seeds a few products on startup so the API can be tried immediately. */
@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }
        productRepository.saveAll(List.of(
                new Product("Laptop", "14-inch business laptop", new BigDecimal("55999.00"), 25),
                new Product("Wireless Mouse", "Ergonomic Bluetooth mouse", new BigDecimal("799.00"), 100),
                new Product("Mechanical Keyboard", "Hot-swappable 87-key keyboard", new BigDecimal("3499.00"), 40)));
    }
}
