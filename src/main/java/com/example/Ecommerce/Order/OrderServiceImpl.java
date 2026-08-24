package com.example.Ecommerce.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.Common.AuthenticationHelper;
import com.example.Ecommerce.Common.Exceptions.InvalidTransitionException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Common.Exceptions.UnauthorizedAccessException;
import com.example.Ecommerce.Order.DTOs.request.ChangeShippingAddressRequest;
import com.example.Ecommerce.Order.DTOs.request.PlaceOrderRequest;
import com.example.Ecommerce.Order.DTOs.request.UpdateOrderStatusRequest;
import com.example.Ecommerce.Order.DTOs.response.ChangeShippingAddressResponse;
import com.example.Ecommerce.Order.DTOs.response.GetOrderByIdResponse;
import com.example.Ecommerce.Order.DTOs.response.OrderItemsResponse;
import com.example.Ecommerce.Order.DTOs.response.PlaceOrderResponse;
import com.example.Ecommerce.Order.DTOs.response.UpdateOrderStatusResponse;
import com.example.Ecommerce.Payment.RazorpayService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private OrderRepository orderRepository;

    private AuthenticationHelper authenticationHelper;
    private OrderTransactionExecutor orderTransactionExecutor;
    private RazorpayService razorpayService;

    public OrderServiceImpl(OrderRepository orderRepository, AuthenticationHelper authenticationHelper,
            OrderTransactionExecutor orderTransactionExecutor, RazorpayService razorpayService) {
        this.orderRepository = orderRepository;

        this.orderTransactionExecutor = orderTransactionExecutor;
        this.authenticationHelper = authenticationHelper;
        this.razorpayService = razorpayService;

    }

    @Override
    public List<GetOrderByIdResponse> getOrdersForCurrentUser() {
        AppUser currentUser = authenticationHelper.getCurrentUser();

        List<Order> orders = orderRepository.findUserOrdersWithItems(currentUser.getUserId());

        // Response
        List<GetOrderByIdResponse> response = new ArrayList<>();

        for (Order order : orders) {
            List<OrderItemsResponse> orderItems = new ArrayList<>();

            for (OrderItem item : order.getOrderItems()) {
                OrderItemsResponse orderItem = new OrderItemsResponse(
                        item.getQuantity(),
                        item.getProduct().getProductId(),
                        item.getPriceAtCheckout());

                orderItems.add(orderItem);
            }

            GetOrderByIdResponse orderResponse = new GetOrderByIdResponse(
                    order.getOrderId(),
                    order.getTotalPrice(),
                    order.getShippingAddress(),
                    order.getOrderDateTime(),
                    order.getOrderStatus(),
                    orderItems);

            response.add(orderResponse);

        }
        return response;

    }

    @Override
    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest request) {

        AppUser currentUser = authenticationHelper.getCurrentUser();

        // If razorpayOrderId is provided, verify payment first
        if (request.getRazorpayOrderId() != null && request.getRazorpayPaymentId() != null 
                && request.getRazorpaySignature() != null) {
            
            boolean isValid = razorpayService.verifyPaymentSignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );
            
            if (!isValid) {
                throw new IllegalArgumentException("Payment verification failed");
            }
        }

        Order savedOrder = orderTransactionExecutor.placeOrderTransactional(request, currentUser);

        List<OrderItemsResponse> orderItemsResponse = new ArrayList<>();

        for (OrderItem item : savedOrder.getOrderItems()) {

            orderItemsResponse
                    .add(new OrderItemsResponse(item.getQuantity(), item.getProduct().getProductId(),
                            item.getPriceAtCheckout()));

        }

        // Response
        PlaceOrderResponse response = new PlaceOrderResponse();

        response.setOrderDateTime(savedOrder.getOrderDateTime());
        response.setShippingAddress(savedOrder.getShippingAddress());
        response.setOrderStatus(savedOrder.getOrderStatus());
        response.setTotalPrice(savedOrder.getTotalPrice());
        response.setOrderItems(orderItemsResponse);
        response.setOrderId(savedOrder.getOrderId());
        response.setRazorpayOrderId(savedOrder.getRazorpayOrderId());
        return response;

    }

    @Override
    @Transactional
    public PlaceOrderResponse verifyPaymentAndConfirmOrder(Long orderId, String razorpayPaymentId, String razorpaySignature) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        AppUser currentUser = authenticationHelper.getCurrentUser();

        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedAccessException("Unauthorized access");
        }

        if (order.getRazorpayOrderId() == null) {
            throw new IllegalStateException("Order does not have a Razorpay order ID");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidTransitionException("Order is not in pending state");
        }

        boolean isValid = razorpayService.verifyPaymentSignature(
                order.getRazorpayOrderId(),
                razorpayPaymentId,
                razorpaySignature
        );

        if (!isValid) {
            throw new IllegalArgumentException("Payment verification failed");
        }

        // Atomic conditional update: only transitions if still PENDING
        int updated = orderRepository.updateStatusIfCurrent(orderId, OrderStatus.PENDING, OrderStatus.PAID);
        if (updated == 0) {
            // Already processed (idempotent) - reload and return current state
            order = orderRepository.findById(orderId).orElseThrow();
        } else {
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setRazorpaySignature(razorpaySignature);
            order.setOrderStatus(OrderStatus.PAID);
            order.setPaymentDateTime(LocalDateTime.now());
        }

        List<OrderItemsResponse> orderItemsResponse = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            orderItemsResponse.add(new OrderItemsResponse(
                    item.getQuantity(), item.getProduct().getProductId(), item.getPriceAtCheckout()));
        }

        PlaceOrderResponse response = new PlaceOrderResponse();
        response.setOrderDateTime(order.getOrderDateTime());
        response.setShippingAddress(order.getShippingAddress());
        response.setOrderStatus(order.getOrderStatus());
        response.setTotalPrice(order.getTotalPrice());
        response.setOrderItems(orderItemsResponse);
        response.setOrderId(order.getOrderId());
        response.setRazorpayOrderId(order.getRazorpayOrderId());
        return response;
    }

    // Called from webhook. Trust comes from the verified X-Razorpay-Signature header
    // over the raw payload - webhook bodies do not carry a payment signature field.
    @Override
    @Transactional
    public void handlePaymentCaptured(String razorpayOrderId, String razorpayPaymentId, Integer amountInPaise) {
        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (order == null) {
            log.warn("Webhook received for unknown Razorpay order: {}", razorpayOrderId);
            return;
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            return; // Already processed, idempotent no-op
        }

        // long math guards the paise conversion against int overflow on large totals
        if (amountInPaise != null && amountInPaise != order.getTotalPrice() * 100L) {
            log.error("Amount mismatch for order {}: expected {} paise but webhook reported {}",
                    order.getOrderId(), order.getTotalPrice() * 100L, amountInPaise);
            return;
        }

        // Atomic conditional update
        int updated = orderRepository.updateStatusIfCurrent(order.getOrderId(), OrderStatus.PENDING, OrderStatus.PAID);
        if (updated > 0) {
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setPaymentDateTime(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Order {} marked PAID via webhook (payment {})", order.getOrderId(), razorpayPaymentId);
        }
    }

    @Override
    @Transactional
    public void cancelOrderAndReleaseStock(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        AppUser currentUser = authenticationHelper.getCurrentUser();

        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedAccessException("Unauthorized access");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return; // Already cancelled - idempotent no-op
        }

        // The executor atomically claims the cancellation (PENDING|PAID -> CANCELLED)
        // and releases stock only if it wins; false means a concurrent request or the
        // expiry job moved the order past a cancellable state.
        boolean cancelled = orderTransactionExecutor.cancelOrderTransactional(
                orderId, EnumSet.of(OrderStatus.PENDING, OrderStatus.PAID));

        if (!cancelled) {
            throw new InvalidTransitionException("Only pending or paid orders can be Cancelled.");
        }
    }

    @Override
    public ChangeShippingAddressResponse changeShippingAddress(Long orderId, ChangeShippingAddressRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with Order id : " + orderId + " not found"));

        AppUser currentUser = authenticationHelper.getCurrentUser();

        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedAccessException("Unauthorized access");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidTransitionException("Shipping address can only be changed for pending orders.");
        }

        order.setShippingAddress(request.getNewShippingAddress());
        orderRepository.save(order);

        // Response
        ChangeShippingAddressResponse response = new ChangeShippingAddressResponse(orderId, order.getShippingAddress());

        return response;

    }

    @Override
    public UpdateOrderStatusResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with orderid : " + orderId + " not found"));

        OrderStatus currentStatus = order.getOrderStatus();

        if (!currentStatus.canTransitionTo(request.getUpdatedStatus())) {
            throw new InvalidTransitionException("An order with status : " + order.getOrderStatus()
                    + " can not transition into " + request.getUpdatedStatus());
        }
        if (request.getUpdatedStatus() == OrderStatus.CANCELLED) {
            boolean cancelled = orderTransactionExecutor.cancelOrderTransactional(
                    orderId, EnumSet.of(OrderStatus.PENDING, OrderStatus.PAID));

            if (!cancelled) {
                // Lost a concurrent race - fail only if the order moved somewhere other than CANCELLED
                Order fresh = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Order with orderid : " + orderId + " not found"));
                if (fresh.getOrderStatus() != OrderStatus.CANCELLED) {
                    throw new InvalidTransitionException("An order with status : " + fresh.getOrderStatus()
                            + " can not transition into " + request.getUpdatedStatus());
                }
            }
        } else {
            order.setOrderStatus(request.getUpdatedStatus());
            orderRepository.save(order);
        }

        // Response
        UpdateOrderStatusResponse response = new UpdateOrderStatusResponse(orderId, request.getUpdatedStatus());
        return response;

    }

    @Override
    public GetOrderByIdResponse getOrderByOrderId(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with orderid : " + orderId + " not found"));

        // Response
        GetOrderByIdResponse response = new GetOrderByIdResponse();

        response.setOrderDateTime(order.getOrderDateTime());
        response.setOrderId(order.getOrderId());
        response.setOrderStatus(order.getOrderStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setTotalPrice(order.getTotalPrice());

        List<OrderItemsResponse> orderItemsResponse = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            OrderItemsResponse orderItemResponse = new OrderItemsResponse();
            orderItemResponse.setPriceAtCheckout(item.getPriceAtCheckout());
            orderItemResponse.setProductId(item.getProduct().getProductId());
            orderItemResponse.setQuantity(item.getQuantity());
            orderItemsResponse.add(orderItemResponse);
        }

        response.setOrderItems(orderItemsResponse);

        return response;
    }

    @Override
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with Order id : " + orderId + " not found"));

        AppUser currentUser = authenticationHelper.getCurrentUser();

        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedAccessException("Unauthorized access");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return order; // Already cancelled - idempotent
        }

        boolean cancelled = orderTransactionExecutor.cancelOrderTransactional(
                orderId, EnumSet.of(OrderStatus.PENDING));

        if (!cancelled) {
            throw new InvalidTransitionException("Only Pending orders can be Cancelled.");
        }

        // The atomic flip detached the originally loaded entity - reload the fresh state
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with Order id : " + orderId + " not found"));

    }

}
