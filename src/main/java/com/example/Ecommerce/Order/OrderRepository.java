package com.example.Ecommerce.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends JpaRepository<Order,Long>   {

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.product
            WHERE o.user.userId = :userId
            """)
    List<Order> findUserOrdersWithItems(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.orderStatus = :newStatus WHERE o.orderId = :orderId AND o.orderStatus = :oldStatus")
    int updateStatusIfCurrent(@Param("orderId") Long orderId, @Param("oldStatus") OrderStatus oldStatus, @Param("newStatus") OrderStatus newStatus);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status AND o.orderDateTime < :cutoff")
    List<Order> findStaleOrders(@Param("status") OrderStatus status, @Param("cutoff") LocalDateTime cutoff);

}
