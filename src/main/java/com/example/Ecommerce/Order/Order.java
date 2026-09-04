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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Table(name="orders")
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private AppUser user; 
    
    private int totalPrice;

    @Column(nullable = true)
    private String addressLine;

    @Column(nullable = true)
    private String pinCode;

    @Column(nullable = true)
    private String landmark;

    @Column(nullable = false)
    private String contactNumber;

    private LocalDateTime orderDateTime;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus=OrderStatus.PENDING;

    @OneToMany(mappedBy = "order",cascade=CascadeType.ALL,orphanRemoval=true)
    @JsonManagedReference
    private List<OrderItem> orderItems;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private LocalDateTime paymentDateTime;

}
