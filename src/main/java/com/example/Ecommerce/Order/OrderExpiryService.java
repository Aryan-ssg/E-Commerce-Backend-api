package com.example.Ecommerce.Order;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    // Runs every 5 minutes.
    // Deliberately NOT @Transactional: each cancellation commits in its own transaction
    // (REQUIRED inside cancelOrderTransactional), so one poisoned order can't roll back
    // or mark rollback-only the whole batch, and the catch below is actually effective.
    @Scheduled(fixedDelay = 300000)
    public void expireStalePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Order> staleOrders = orderRepository.findStaleOrders(OrderStatus.PENDING, cutoff);

        if (staleOrders.isEmpty()) {
            return;
        }

        log.info("Found {} stale PENDING orders older than 15 minutes, cancelling and releasing stock", staleOrders.size());

        for (Order order : staleOrders) {
            try {
                // PENDING-only claim: if the customer paid between our scan and now,
                // the atomic flip fails safely instead of cancelling a PAID order
                boolean cancelled = orderTransactionExecutor.cancelOrderTransactional(
                        order.getOrderId(), EnumSet.of(OrderStatus.PENDING));

                if (cancelled) {
                    log.info("Cancelled stale order {} and released stock", order.getOrderId());
                } else {
                    log.info("Skipped stale order {} - status changed since scan", order.getOrderId());
                }
            } catch (Exception e) {
                log.error("Failed to cancel stale order {}: {}", order.getOrderId(), e.getMessage(), e);
            }
        }
    }
}