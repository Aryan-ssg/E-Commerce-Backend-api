package com.example.Ecommerce.Order;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.Ecommerce.Inventory.StockService;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTransactionExecutorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockService stockService;

    @InjectMocks
    private OrderTransactionExecutor executor;

    private Product product(Long productId) {
        Product p = new Product();
        p.setProductId(productId);
        return p;
    }

    private OrderItem item(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void cancelOrderTransactional_claimWon_releasesStockForEveryItem() {
        Order order = new Order();
        order.setOrderId(10L);
        order.setOrderItems(List.of(item(product(1L), 2), item(product(2L), 3)));

        when(orderRepository.updateStatusIfIn(
                eq(10L), eq(EnumSet.of(OrderStatus.PENDING)), eq(OrderStatus.CANCELLED))).thenReturn(1);
        when(orderRepository.findWithItemsById(10L)).thenReturn(Optional.of(order));

        boolean cancelled = executor.cancelOrderTransactional(10L, EnumSet.of(OrderStatus.PENDING));

        assertThat(cancelled).isTrue();
        verify(stockService).releaseStock(1L, 2);
        verify(stockService).releaseStock(2L, 3);
    }

    @Test
    void cancelOrderTransactional_claimLost_doesNotReleaseStock() {
        when(orderRepository.updateStatusIfIn(eq(10L), any(), eq(OrderStatus.CANCELLED))).thenReturn(0);

        boolean cancelled = executor.cancelOrderTransactional(10L, EnumSet.of(OrderStatus.PENDING));

        assertThat(cancelled).isFalse();
        verifyNoInteractions(stockService);
        verify(orderRepository, never()).findWithItemsById(any());
    }
}
