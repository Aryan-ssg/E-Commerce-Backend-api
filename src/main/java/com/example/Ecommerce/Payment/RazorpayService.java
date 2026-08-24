package com.example.Ecommerce.Payment;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import jakarta.annotation.PostConstruct;
import org.json.JSONObject;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        try {
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            throw new IllegalStateException("Failed to initialize Razorpay client", e);
        }
    }

    public Order createOrder(int amountInPaise, String currency, String receipt, Map<String, Object> notes) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);
            if (notes != null && !notes.isEmpty()) {
                orderRequest.put("notes", new JSONObject(notes));
            }
            return razorpayClient.Orders.create(orderRequest);
        } catch (RazorpayException e) {
            throw new PaymentException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("razorpay_order_id", orderId);
            payload.put("razorpay_payment_id", paymentId);
            payload.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature(payload, keySecret);
        } catch (RazorpayException e) {
            throw new PaymentException("Failed to verify payment signature: " + e.getMessage(), e);
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (RazorpayException e) {
            throw new PaymentException("Failed to verify webhook signature: " + e.getMessage(), e);
        }
    }

    public String getKeyId() {
        return keyId;
    }

    public static class PaymentException extends RuntimeException {
        public PaymentException(String message) {
            super(message);
        }

        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}