package com.example.Ecommerce.Order;

import java.util.List;

import com.example.Ecommerce.Order.DTOs.request.ChangeShippingAddressRequest;
import com.example.Ecommerce.Order.DTOs.request.PlaceOrderRequest;
import com.example.Ecommerce.Order.DTOs.request.UpdateOrderStatusRequest;
import com.example.Ecommerce.Order.DTOs.response.ChangeShippingAddressResponse;
import com.example.Ecommerce.Order.DTOs.response.GetOrderByIdResponse;
import com.example.Ecommerce.Order.DTOs.response.PlaceOrderResponse;
import com.example.Ecommerce.Order.DTOs.response.AdminOrderResponse;
import com.example.Ecommerce.Order.DTOs.response.UpdateOrderStatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    List<GetOrderByIdResponse> getOrdersForCurrentUser();

    PlaceOrderResponse placeOrder(PlaceOrderRequest request);

    PlaceOrderResponse verifyPaymentAndConfirmOrder(Long orderId, String razorpayPaymentId, String razorpaySignature);

    void handlePaymentCaptured(String razorpayOrderId, String razorpayPaymentId, Integer amountInPaise);

    void cancelOrderAndReleaseStock(Long orderId);

    ChangeShippingAddressResponse changeShippingAddress(Long orderId, ChangeShippingAddressRequest request);

    UpdateOrderStatusResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    GetOrderByIdResponse getOrderByOrderId(Long orderId);

    Order cancelOrder(Long orderId);

    Page<AdminOrderResponse> getAllOrders(Pageable pageable);

}
