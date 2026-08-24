package com.example.Ecommerce.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        order.setShippingAddress(request.getShippingAddress());
        order.setOrderStatus(OrderStatus.PENDING);
        
        if (request.getRazorpayOrderId() != null) {
            order.setRazorpayOrderId(request.getRazorpayOrderId());
        }

        int totalOrderPrice = 0;
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

            totalOrderPrice += product.getProductPrice() * orderItem.getQuantity();
            itemList.add(item);
        }

        order.setOrderItems(itemList);
        order.setTotalPrice(totalOrderPrice);

        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrderTransactional(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with Order id : " + orderId + " not found"));

        for (OrderItem item : order.getOrderItems()) {
            stockService.releaseStock(item.getProduct().getProductId(), item.getQuantity());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}