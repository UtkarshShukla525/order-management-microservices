package com.utkarsh.ordermgmt.order.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested) {
        super("Not enough stock for product " + productId + " to order " + requested + " unit(s)");
    }
}
