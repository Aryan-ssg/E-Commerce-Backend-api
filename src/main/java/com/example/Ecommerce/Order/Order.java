package com.example.Ecommerce.Order;

import java.time.LocalDateTime;
import java.util.List;

import com.example.Ecommerce.AppUser.AppUser;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private AppUser user; 
    
    private int totalPrice;

    @NotBlank(message="Address is required")
    private String shippingAddress;


    private LocalDateTime orderDateTime;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus=OrderStatus.PENDING;

    @OneToMany(mappedBy = "order",cascade=CascadeType.ALL,orphanRemoval=true)
    @JsonManagedReference
    private List<OrderItem> orderItems;

}
