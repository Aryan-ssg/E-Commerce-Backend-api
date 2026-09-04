package com.example.Ecommerce.Order.DTOs.response;

import java.time.LocalDateTime;
import java.util.List;


import com.example.Ecommerce.Order.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderResponse {

    private Long orderId;

    private int totalPrice;

    private String addressLine;

    private String pinCode;

    private String landmark;

    private String contactNumber;

    private LocalDateTime orderDateTime;

    private OrderStatus orderStatus ;

    private List<OrderItemsResponse> orderItems;
    
    private String razorpayOrderId;
    private String razorpayKeyId;

}
