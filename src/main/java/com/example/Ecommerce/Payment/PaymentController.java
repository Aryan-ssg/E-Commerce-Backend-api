package com.example.Ecommerce.Payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecommerce.Common.AuthenticationHelper;
import com.example.Ecommerce.Order.Order;
import com.example.Ecommerce.Order.OrderRepository;
import com.example.Ecommerce.Order.OrderService;
import com.example.Ecommerce.Payment.DTOs.request.CreatePaymentOrderRequest;
import com.example.Ecommerce.Payment.DTOs.request.VerifyPaymentRequest;
import com.example.Ecommerce.Payment.DTOs.response.CreatePaymentOrderResponse;
import com.example.Ecommerce.Payment.DTOs.response.VerifyPaymentResponse;

import jakarta.validation.Valid;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final AuthenticationHelper authenticationHelper;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public PaymentController(RazorpayService razorpayService, AuthenticationHelper authenticationHelper,
            OrderRepository orderRepository, OrderService orderService) {
        this.razorpayService = razorpayService;
        this.authenticationHelper = authenticationHelper;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/create-order")
    public ResponseEntity<CreatePaymentOrderResponse> createPaymentOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        
        Long userId = authenticationHelper.getCurrentUserId();
        
        // Look up the order and verify ownership
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));
        
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to current user");
        }
        
        if (order.getOrderStatus() != com.example.Ecommerce.Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Order is not in pending state");
        }
        
        String receipt = "order_rcpt_" + order.getOrderId() + "_" + System.currentTimeMillis();
        
        // Compute amount server-side from order total (in rupees, convert to paise)
        int amountInPaise = order.getTotalPrice() * 100;
        
        com.razorpay.Order razorpayOrder = razorpayService.createOrder(
                amountInPaise, 
                request.getCurrency(), 
                receipt, 
                null
        );
        
        // Store razorpay order ID on the order
        order.setRazorpayOrderId(razorpayOrder.get("id"));
        orderRepository.save(order);
        
        CreatePaymentOrderResponse response = new CreatePaymentOrderResponse(
                razorpayOrder.get("id"),
                amountInPaise,
                request.getCurrency(),
                razorpayService.getKeyId(),
                receipt
        );
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/verify")
    public ResponseEntity<VerifyPaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        
        boolean isValid = razorpayService.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        
        if (isValid) {
            return ResponseEntity.ok(new VerifyPaymentResponse(
                    true, 
                    "Payment verified successfully", 
                    request.getRazorpayOrderId()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new VerifyPaymentResponse(
                    false, 
                    "Payment verification failed", 
                    request.getRazorpayOrderId()
            ));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        boolean isValid = razorpayService.verifyWebhookSignature(payload, signature);
        
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");
            
            if ("payment.captured".equals(eventType)) {
                JSONObject paymentEntity = event.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");
                
                String razorpayOrderId = paymentEntity.getString("order_id");
                String razorpayPaymentId = paymentEntity.getString("id");
                Integer amountInPaise = paymentEntity.has("amount") ? paymentEntity.getInt("amount") : null;
                
                orderService.handlePaymentCaptured(razorpayOrderId, razorpayPaymentId, amountInPaise);
            }
            // Handle other events like payment.failed if needed
        } catch (Exception e) {
            log.error("Failed to process Razorpay webhook payload", e);
        }
        
        return ResponseEntity.ok().build();
    }
}