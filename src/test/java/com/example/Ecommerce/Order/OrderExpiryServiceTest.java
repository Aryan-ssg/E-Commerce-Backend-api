package com.example.Ecommerce.Order;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExpiryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTransactionExecutor orderTransactionExecutor;

    @InjectMocks
    private OrderExpiryService expiryService;

    private Order staleOrder(Long orderId) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderStatus(OrderStatus.PENDING);
        return order;
    }

    @Test
    void expireStalePendingOrders_cancelsEachStaleOrderWithPendingOnlyClaim() {
        when(orderRepository.findStaleOrders(eq(OrderStatus.PENDING), any()))
                .thenReturn(List.of(staleOrder(11L), staleOrder(12L)));
        when(orderTransactionExecutor.cancelOrderTransactional(eq(11L), eq(EnumSet.of(OrderStatus.PENDING))))
                .thenReturn(true);
        when(orderTransactionExecutor.cancelOrderTransactional(eq(12L), eq(EnumSet.of(OrderStatus.PENDING))))
                .thenReturn(true);

        expiryService.expireStalePendingOrders();

        verify(orderTransactionExecutor).cancelOrderTransactional(11L, EnumSet.of(OrderStatus.PENDING));
        verify(orderTransactionExecutor).cancelOrderTransactional(12L, EnumSet.of(OrderStatus.PENDING));
        // No direct writes from the scheduler - the executor owns the state change
        verify(orderRepository, never()).save(any());
    }

    @Test
    void expireStalePendingOrders_onePoisonedOrderDoesNotStopTheBatch() {
        when(orderRepository.findStaleOrders(eq(OrderStatus.PENDING), any()))
                .thenReturn(List.of(staleOrder(11L), staleOrder(12L)));
        doThrow(new RuntimeException("DB connection lost"))
                .when(orderTransactionExecutor)
                .cancelOrderTransactional(eq(11L), eq(EnumSet.of(OrderStatus.PENDING)));
        when(orderTransactionExecutor.cancelOrderTransactional(eq(12L), eq(EnumSet.of(OrderStatus.PENDING))))
                .thenReturn(true);

        expiryService.expireStalePendingOrders();

        verify(orderTransactionExecutor).cancelOrderTransactional(12L, EnumSet.of(OrderStatus.PENDING));
    }
}
