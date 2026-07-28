package com.example.Ecommerce.Order;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecommerce.Order.DTOs.request.ChangeShippingAddressRequest;
import com.example.Ecommerce.Order.DTOs.request.PlaceOrderRequest;
import com.example.Ecommerce.Order.DTOs.request.UpdateOrderStatusRequest;
import com.example.Ecommerce.Order.DTOs.response.ChangeShippingAddressResponse;
import com.example.Ecommerce.Order.DTOs.response.GetOrderByIdResponse;
import com.example.Ecommerce.Order.DTOs.response.PlaceOrderResponse;
import com.example.Ecommerce.Order.DTOs.response.UpdateOrderStatusResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class OrderController {

    private OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/order/my-orders")
    public ResponseEntity<List<GetOrderByIdResponse>> getOrdersForCurrentUser(){
        List<GetOrderByIdResponse> orderList=  orderService.getOrdersForCurrentUser();
        return ResponseEntity.status(HttpStatus.OK).body(orderList);
    }

    @PostMapping("/order/place")
    public ResponseEntity<PlaceOrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {

        PlaceOrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/order/{orderId}/changeShippingAddress")
    public ResponseEntity<ChangeShippingAddressResponse> changeShippingAddress(@PathVariable Long orderId,
            @Valid @RequestBody ChangeShippingAddressRequest request) {

        ChangeShippingAddressResponse response = orderService.changeShippingAddress(orderId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/order/{orderId}/updateOrderStatus")
    public ResponseEntity<UpdateOrderStatusResponse> updateOrderStatus(@PathVariable Long orderId,
           @Valid @RequestBody UpdateOrderStatusRequest request) {

        UpdateOrderStatusResponse response = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

   
   
    @PutMapping("/order/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable Long orderId){

        Order response=orderService.cancelOrder(orderId);
        if(response==null){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Only Pending orders can be cancelled");

        }
        return ResponseEntity.status(HttpStatus.OK).body("Order has been cancelled");


    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<GetOrderByIdResponse> getOrderByOrderId(@PathVariable Long orderId) {
        GetOrderByIdResponse response = orderService.getOrderByOrderId(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
