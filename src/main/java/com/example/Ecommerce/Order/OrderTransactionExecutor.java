package com.example.Ecommerce.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;

import com.example.Ecommerce.Inventory.StockService;
import com.example.Ecommerce.Order.DTOs.request.OrderItemsRequest;
import com.example.Ecommerce.Order.DTOs.request.PlaceOrderRequest;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;

@Component
class OrderTransactionExecutor {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private StockService stockService;

    OrderTransactionExecutor(OrderRepository orderRepository, ProductRepository productRepository,
            StockService stockService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
    }

    @Transactional
    public Order placeOrderTransactional(PlaceOrderRequest request, AppUser user) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderDateTime(LocalDateTime.now());
        order.setAddressLine(request.getAddressLine());
        order.setPinCode(request.getPinCode());
        order.setLandmark(request.getLandmark());
        order.setContactNumber(request.getContactNumber());
        order.setOrderStatus(OrderStatus.PENDING);
        
        if (request.getRazorpayOrderId() != null) {
            order.setRazorpayOrderId(request.getRazorpayOrderId());
        }

        long totalOrderPrice = 0;
        List<OrderItem> itemList = new ArrayList<>();

        for (OrderItemsRequest orderItem : request.getOrderItems()) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product with productid : " + orderItem.getProductId() + " not found"));
                            
            // Atomically checks and decrements stock in one UPDATE statement
            // (see ProductRepository.decrementStockIfAvailable) - throws
            // InsufficientStockException if not enough stock was available.
            stockService.reserveStock(product.getProductId(), orderItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(orderItem.getQuantity());
            item.setPriceAtCheckout(product.getProductPrice());

            // long arithmetic guards the running sum against int overflow on large carts
            totalOrderPrice += (long) product.getProductPrice() * orderItem.getQuantity();
            itemList.add(item);
        }

        if (totalOrderPrice > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Order total exceeds the maximum supported amount");
        }

        order.setOrderItems(itemList);
        order.setTotalPrice((int) totalOrderPrice);

        return orderRepository.save(order);
    }

    /**
     * Atomically cancels an order and releases its reserved stock.
     * <p>
     * The status flip is a single conditional UPDATE, so exactly one racing caller
     * (user cancel, admin cancel, expiry job) can win the claim. Stock is released
     * only by the winner, inside the same transaction - if any release fails, the
     * flip rolls back too. Callers that lose the race get {@code false} instead of
     * silently double-releasing inventory.
     *
     * @param allowedFromStatuses states the order may currently be in to qualify for cancellation;
     *                            the expiry job passes only PENDING so it can never cancel a just-paid order
     * @return true if this caller performed the cancellation, false if the order was in another state
     */
    @Transactional
    public boolean cancelOrderTransactional(Long orderId, Collection<OrderStatus> allowedFromStatuses) {
        int updated = orderRepository.updateStatusIfIn(orderId, allowedFromStatuses, OrderStatus.CANCELLED);

        if (updated == 0) {
            return false;
        }

        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order disappeared during cancellation: " + orderId));

        for (OrderItem item : order.getOrderItems()) {
            stockService.releaseStock(item.getProduct().getProductId(), item.getQuantity());
        }

        return true;
    }
}