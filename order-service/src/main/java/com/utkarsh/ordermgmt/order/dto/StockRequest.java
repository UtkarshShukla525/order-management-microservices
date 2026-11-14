package com.utkarsh.ordermgmt.order.dto;

/** Request body sent to product-service's reserve/release endpoints. */
public record StockRequest(int quantity) {
}
