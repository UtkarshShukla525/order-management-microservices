package com.utkarsh.ordermgmt.order.service;

import com.utkarsh.ordermgmt.order.client.ProductClient;
import com.utkarsh.ordermgmt.order.dto.OrderRequest;
import com.utkarsh.ordermgmt.order.dto.OrderResponse;
import com.utkarsh.ordermgmt.order.dto.ProductDto;
import com.utkarsh.ordermgmt.order.exception.InsufficientStockException;
import com.utkarsh.ordermgmt.order.exception.InvalidOrderStateException;
import com.utkarsh.ordermgmt.order.exception.OrderNotFoundException;
import com.utkarsh.ordermgmt.order.model.Order;
import com.utkarsh.ordermgmt.order.model.OrderStatus;
import com.utkarsh.ordermgmt.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_reservesStockAndSavesOrderWithTotal() {
        when(productClient.reserveStock(1L, 2))
                .thenReturn(new ProductDto(1L, "Laptop", new BigDecimal("1000.00"), 8));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        OrderResponse response = orderService.placeOrder(new OrderRequest(1L, 2));

        assertEquals(10L, response.id());
        assertEquals("Laptop", response.productName());
        assertEquals(new BigDecimal("2000.00"), response.totalPrice());
        assertEquals(OrderStatus.PLACED, response.status());
    }

    @Test
    void placeOrder_whenStockInsufficient_propagatesAndSavesNothing() {
        when(productClient.reserveStock(1L, 50)).thenThrow(new InsufficientStockException(1L, 50));

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(new OrderRequest(1L, 50)));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_whenSaveFails_releasesReservedStock() {
        when(productClient.reserveStock(1L, 2))
                .thenReturn(new ProductDto(1L, "Laptop", new BigDecimal("1000.00"), 8));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("database down"));

        assertThrows(RuntimeException.class, () -> orderService.placeOrder(new OrderRequest(1L, 2)));

        verify(productClient).releaseStock(1L, 2);
    }

    @Test
    void cancelOrder_releasesStockAndMarksCancelled() {
        Order existing = new Order(1L, "Laptop", 2, new BigDecimal("1000.00"),
                new BigDecimal("2000.00"), OrderStatus.PLACED);
        existing.setId(5L);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).then(returnsFirstArg());

        OrderResponse response = orderService.cancelOrder(5L);

        assertEquals(OrderStatus.CANCELLED, response.status());
        verify(productClient).releaseStock(1L, 2);
    }

    @Test
    void cancelOrder_whenAlreadyCancelled_throwsAndDoesNotReleaseStockAgain() {
        Order existing = new Order(1L, "Laptop", 2, new BigDecimal("1000.00"),
                new BigDecimal("2000.00"), OrderStatus.CANCELLED);
        existing.setId(5L);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(InvalidOrderStateException.class, () -> orderService.cancelOrder(5L));

        verify(productClient, never()).releaseStock(anyLong(), anyInt());
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.findById(99L));
    }
}
