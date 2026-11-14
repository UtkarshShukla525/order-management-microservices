package com.utkarsh.ordermgmt.order.service;

import com.utkarsh.ordermgmt.order.client.ProductClient;
import com.utkarsh.ordermgmt.order.dto.OrderRequest;
import com.utkarsh.ordermgmt.order.dto.OrderResponse;
import com.utkarsh.ordermgmt.order.dto.ProductDto;
import com.utkarsh.ordermgmt.order.exception.InvalidOrderStateException;
import com.utkarsh.ordermgmt.order.exception.OrderNotFoundException;
import com.utkarsh.ordermgmt.order.model.Order;
import com.utkarsh.ordermgmt.order.model.OrderStatus;
import com.utkarsh.ordermgmt.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    /**
     * Places an order: first reserve the stock in product-service, then save the
     * order. If saving fails after stock was taken, the stock is given back
     * (a simple compensating action, since the two services have separate databases
     * and cannot share one transaction).
     */
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        int quantity = request.quantity();
        ProductDto product = productClient.reserveStock(request.productId(), quantity);

        BigDecimal total = product.price().multiply(BigDecimal.valueOf(quantity));
        Order order = new Order(product.id(), product.name(), quantity, product.price(), total, OrderStatus.PLACED);

        try {
            return OrderResponse.from(orderRepository.save(order));
        } catch (RuntimeException ex) {
            log.error("Saving order failed after reserving stock for product {}; releasing stock", product.id(), ex);
            productClient.releaseStock(product.id(), quantity);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrderOrThrow(id));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = getOrderOrThrow(id);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order " + id + " is already cancelled");
        }

        productClient.releaseStock(order.getProductId(), order.getQuantity());
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
