package com.utkarsh.ordermgmt.product.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long id, int available, int requested) {
        super("Insufficient stock for product " + id
                + ": requested " + requested + ", available " + available);
    }
}
