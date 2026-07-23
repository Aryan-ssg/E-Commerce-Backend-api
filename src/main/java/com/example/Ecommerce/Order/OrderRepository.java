package com.example.Ecommerce.Order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order,Long>   {

    List<Order> findByUser_UserId(Long userId);
}
