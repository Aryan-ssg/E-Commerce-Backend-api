package com.example.Ecommerce.Order;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderExpiryService {

    private final OrderRepository orderRepository;
    private final OrderTransactionExecutor orderTransactionExecutor;

    public OrderExpiryService(OrderRepository orderRepository, OrderTransactionExecutor orderTransactionExecutor) {
        this.orderRepository = orderRepository;
        this.orderTransactionExecutor = orderTransactionExecutor;
    }

    // Runs every 5 minutes
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void expireStalePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Order> staleOrders = orderRepository.findStaleOrders(OrderStatus.PENDING, cutoff);

        if (staleOrders.isEmpty()) {
            return;
        }

        log.info("Found {} stale PENDING orders older than 15 minutes, cancelling and releasing stock", staleOrders.size());

        for (Order order : staleOrders) {
            try {
                // Release stock
                orderTransactionExecutor.cancelOrderTransactional(order.getOrderId());
                order.setOrderStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("Cancelled stale order {} and released stock", order.getOrderId());
            } catch (Exception e) {
                log.error("Failed to cancel stale order {}: {}", order.getOrderId(), e.getMessage(), e);
            }
        }
    }
}