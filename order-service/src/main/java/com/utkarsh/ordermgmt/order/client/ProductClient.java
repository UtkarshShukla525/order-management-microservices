package com.utkarsh.ordermgmt.order.client;

import com.utkarsh.ordermgmt.order.dto.ProductDto;
import com.utkarsh.ordermgmt.order.dto.StockRequest;
import com.utkarsh.ordermgmt.order.exception.InsufficientStockException;
import com.utkarsh.ordermgmt.order.exception.ProductNotFoundException;
import com.utkarsh.ordermgmt.order.exception.ProductServiceUnavailableException;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Synchronous REST client for product-service. "product-service" is not a real
 * hostname: the load-balanced builder looks it up in Eureka at call time.
 */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(@LoadBalanced RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://product-service/api/products")
                .build();
    }

    /** Takes stock out of inventory and returns the product (with its current price). */
    public ProductDto reserveStock(Long productId, int quantity) {
        return postStockChange(productId, "reserve", quantity);
    }

    /** Puts stock back into inventory. */
    public void releaseStock(Long productId, int quantity) {
        postStockChange(productId, "release", quantity);
    }

    private ProductDto postStockChange(Long productId, String action, int quantity) {
        try {
            return restClient.post()
                    .uri("/{id}/{action}", productId, action)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockRequest(quantity))
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ProductNotFoundException(productId);
                    })
                    .onStatus(status -> status.value() == 409, (request, response) -> {
                        throw new InsufficientStockException(productId, quantity);
                    })
                    .body(ProductDto.class);
        } catch (ResourceAccessException ex) {
            // Connection refused or timeout (for example a stale Eureka entry).
            throw new ProductServiceUnavailableException(ex);
        } catch (IllegalStateException ex) {
            // Spring Cloud LoadBalancer throws this when no instance of the service is
            // registered in Eureka. Anything else is a real bug and must not be hidden.
            if (ex.getMessage() != null && ex.getMessage().startsWith("No instances available")) {
                throw new ProductServiceUnavailableException(ex);
            }
            throw ex;
        }
    }
}
