package com.utkarsh.ordermgmt.order.exception;

public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(Throwable cause) {
        super("Product service is currently unavailable, please try again later", cause);
    }
}
