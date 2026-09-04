package com.example.Ecommerce.Payment;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.AppUser.AppUserRepository;
import com.example.Ecommerce.AppUser.Role;
import com.example.Ecommerce.Common.AuthenticationHelper;
import com.example.Ecommerce.Common.JwtUtils;
import com.example.Ecommerce.Common.JwtAuthenticationEntryPoint;
import com.example.Ecommerce.Common.RefreshTokenService;
import com.example.Ecommerce.Common.SecurityConfig;
import com.example.Ecommerce.Order.Order;
import com.example.Ecommerce.Order.OrderRepository;
import com.example.Ecommerce.Order.OrderService;
import com.example.Ecommerce.Order.OrderStatus;
import com.example.Ecommerce.Payment.DTOs.response.CreatePaymentOrderResponse;
import com.example.Ecommerce.Payment.DTOs.response.VerifyPaymentResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RazorpayService razorpayService;

    @MockitoBean
    private AuthenticationHelper authenticationHelper;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private AppUserRepository appUserRepository;

    private AppUser buildUser(Long userId) {
        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setUsername("shopper");
        user.setRole(Role.USER);
        return user;
    }

    private Order buildOrder(Long orderId, AppUser user, OrderStatus status) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUser(user);
        order.setOrderStatus(status);
        order.setTotalPrice(4000);
        order.setAddressLine("123 Main St");
        order.setPinCode("110001");
        return order;
    }

    // ── POST /api/payment/create-order ───────────────────────────────────

    @Test
    void createPaymentOrder_validRequest_returns200() throws Exception {
        AppUser user = buildUser(1L);
        Order order = buildOrder(10L, user, OrderStatus.PENDING);
        when(authenticationHelper.getCurrentUserId()).thenReturn(1L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        com.razorpay.Order razorpayOrder = org.mockito.Mockito.mock(com.razorpay.Order.class);
        when(razorpayOrder.get("id")).thenReturn("order_abc123");
        when(razorpayService.createOrder(eq(400000), eq("INR"), any(), any())).thenReturn(razorpayOrder);
        when(razorpayService.getKeyId()).thenReturn("rzp_test_key");

        mockMvc.perform(post("/api/payment/create-order")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":10,\"currency\":\"INR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_abc123"))
                .andExpect(jsonPath("$.amount").value(400000))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.keyId").value("rzp_test_key"));

        verify(orderRepository).save(order);
    }

    @Test
    void createPaymentOrder_orderNotFound_returns400() throws Exception {
        when(authenticationHelper.getCurrentUserId()).thenReturn(1L);
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/payment/create-order")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":99,\"currency\":\"INR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Order not found")));

        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrder_orderNotOwnedByUser_returns400() throws Exception {
        AppUser owner = buildUser(1L);
        AppUser other = buildUser(2L);
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(authenticationHelper.getCurrentUserId()).thenReturn(2L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        mockMvc.perform(post("/api/payment/create-order")
                        .with(user("intruder").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":10,\"currency\":\"INR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("does not belong")));

        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrder_orderNotPending_returns400() throws Exception {
        AppUser user = buildUser(1L);
        Order order = buildOrder(10L, user, OrderStatus.SHIPPED);
        when(authenticationHelper.getCurrentUserId()).thenReturn(1L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        mockMvc.perform(post("/api/payment/create-order")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":10,\"currency\":\"INR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("pending")));

        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrder_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/api/payment/create-order")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"INR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(razorpayService);
    }

    @Test
    void createPaymentOrder_adminRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/payment/create-order")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":10,\"currency\":\"INR\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(razorpayService);
    }

    // ── POST /api/payment/verify ─────────────────────────────────────────

    @Test
    void verifyPayment_validSignature_returns200() throws Exception {
        when(razorpayService.verifyPaymentSignature("order_abc", "pay_123", "sig_xyz"))
                .thenReturn(true);

        mockMvc.perform(post("/api/payment/verify")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId": "order_abc",
                                  "razorpayPaymentId": "pay_123",
                                  "razorpaySignature": "sig_xyz"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment verified successfully"))
                .andExpect(jsonPath("$.orderId").value("order_abc"));
    }

    @Test
    void verifyPayment_invalidSignature_returns400() throws Exception {
        when(razorpayService.verifyPaymentSignature("order_abc", "pay_123", "bad_sig"))
                .thenReturn(false);

        mockMvc.perform(post("/api/payment/verify")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId": "order_abc",
                                  "razorpayPaymentId": "pay_123",
                                  "razorpaySignature": "bad_sig"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("failed")));
    }

    @Test
    void verifyPayment_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/payment/verify")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(razorpayService);
    }

    // ── POST /api/payment/webhook ────────────────────────────────────────

    @Test
    void handleWebhook_validSignature_paymentCaptured_callsService() throws Exception {
        String payload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "order_id": "order_abc123",
                        "id": "pay_123",
                        "amount": 400000
                      }
                    }
                  }
                }
                """;
        when(razorpayService.verifyWebhookSignature(payload, "valid_sig")).thenReturn(true);

        mockMvc.perform(post("/api/payment/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("X-Razorpay-Signature", "valid_sig"))
                .andExpect(status().isOk());

        verify(orderService).handlePaymentCaptured("order_abc123", "pay_123", 400000);
    }

    @Test
    void handleWebhook_invalidSignature_returns400() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";
        when(razorpayService.verifyWebhookSignature(payload, "bad_sig")).thenReturn(false);

        mockMvc.perform(post("/api/payment/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("X-Razorpay-Signature", "bad_sig"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).handlePaymentCaptured(any(), any(), any());
    }

    @Test
    void handleWebhook_validSignature_nonPaymentEvent_returns200() throws Exception {
        String payload = """
                {
                  "event": "order.paid",
                  "payload": {}
                }
                """;
        when(razorpayService.verifyWebhookSignature(payload, "valid_sig")).thenReturn(true);

        mockMvc.perform(post("/api/payment/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("X-Razorpay-Signature", "valid_sig"))
                .andExpect(status().isOk());

        verify(orderService, never()).handlePaymentCaptured(any(), any(), any());
    }
}
