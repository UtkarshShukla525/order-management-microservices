package com.utkarsh.ordermgmt.order.repository;

import com.utkarsh.ordermgmt.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
