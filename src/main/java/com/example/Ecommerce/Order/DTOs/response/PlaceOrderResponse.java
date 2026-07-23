package com.example.Ecommerce.Order.DTOs.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.Order.OrderItem;
import com.example.Ecommerce.Order.OrderStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
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

    private String shippingAddress;

    private LocalDateTime orderDateTime;

    private OrderStatus orderStatus ;

    private List<OrderItemsResponse> orderItems;

}
