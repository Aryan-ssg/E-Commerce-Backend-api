package com.example.Ecommerce.Order;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.Ecommerce.AppUser.AppUserRepository;
import com.example.Ecommerce.Common.Exceptions.InvalidTransitionException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Common.Exceptions.UnauthorizedAccessException;
import com.example.Ecommerce.Common.JwtUtils;
import com.example.Ecommerce.Common.JwtAuthenticationEntryPoint;
import com.example.Ecommerce.Common.RefreshTokenService;
import com.example.Ecommerce.Common.SecurityConfig;
import com.example.Ecommerce.Order.DTOs.response.GetOrderByIdResponse;
import com.example.Ecommerce.Order.DTOs.response.OrderItemsResponse;
import com.example.Ecommerce.Order.DTOs.response.PlaceOrderResponse;
import com.example.Ecommerce.Order.DTOs.response.ChangeShippingAddressResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    // ── Helpers ──────────────────────────────────────────────────────────

    private PlaceOrderResponse buildPlaceResponse(Long orderId, String contactNumber) {
        PlaceOrderResponse r = new PlaceOrderResponse();
        r.setOrderId(orderId);
        r.setTotalPrice(4000);
        r.setAddressLine("123 Main St");
        r.setPinCode("110001");
        r.setLandmark("Near park");
        r.setContactNumber(contactNumber);
        r.setOrderDateTime(LocalDateTime.now());
        r.setOrderStatus(OrderStatus.PENDING);
        r.setOrderItems(List.of(new OrderItemsResponse(2, 100L, 2000)));
        r.setRazorpayOrderId("order_abc123");
        return r;
    }

    private GetOrderByIdResponse buildGetOrderResponse(Long orderId, String contactNumber) {
        GetOrderByIdResponse r = new GetOrderByIdResponse();
        r.setOrderId(orderId);
        r.setTotalPrice(4000);
        r.setAddressLine("123 Main St");
        r.setPinCode("110001");
        r.setLandmark("Near park");
        r.setContactNumber(contactNumber);
        r.setOrderDateTime(LocalDateTime.now());
        r.setOrderStatus(OrderStatus.PENDING);
        r.setOrderItems(List.of(new OrderItemsResponse(2, 100L, 2000)));
        return r;
    }

    private ChangeShippingAddressResponse buildChangeAddressResponse(Long orderId, String contactNumber) {
        ChangeShippingAddressResponse r = new ChangeShippingAddressResponse();
        r.setOrderId(orderId);
        r.setAddressLine("456 New Ave");
        r.setPinCode("110002");
        r.setLandmark(null);
        r.setContactNumber(contactNumber);
        return r;
    }

    private static final String VALID_BODY = """
            {
              "orderItems": [{"productId": 100, "quantity": 2}],
              "addressLine": "123 Main St",
              "pinCode": "110001",
              "landmark": "Near park",
              "contactNumber": "9876543210"
            }
            """;

    // ── POST /api/order/place ────────────────────────────────────────────

    @Test
    void placeOrder_validRequest_returns201WithOrder() throws Exception {
        when(orderService.placeOrder(any()))
                .thenReturn(buildPlaceResponse(1L, "9876543210"));

        mockMvc.perform(post("/api/order/place")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.contactNumber").value("9876543210"))
                .andExpect(jsonPath("$.addressLine").value("123 Main St"))
                .andExpect(jsonPath("$.pinCode").value("110001"));
    }

    @Test
    void placeOrder_adminRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/order/place")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    // ── GET /api/order/my-orders ─────────────────────────────────────────

    @Test
    void getMyOrders_authenticatedUser_returns200WithOrders() throws Exception {
        when(orderService.getOrdersForCurrentUser())
                .thenReturn(List.of(buildGetOrderResponse(1L, "9876543210")));

        mockMvc.perform(get("/api/order/my-orders").with(user("shopper").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1))
                .andExpect(jsonPath("$[0].contactNumber").value("9876543210"));
    }

    @Test
    void getMyOrders_adminRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/order/my-orders").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    // ── PUT /api/order/{orderId}/cancel ──────────────────────────────────

    @Test
    void cancelOrder_ownerOnPendingOrder_returns200() throws Exception {
        mockMvc.perform(put("/api/order/1/cancel").with(user("shopper").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void cancelOrder_orderNotFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Order not found: 99"))
                .when(orderService).cancelOrderAndReleaseStock(99L);

        mockMvc.perform(put("/api/order/99/cancel").with(user("shopper").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void cancelOrder_notCancellable_returns403() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidTransitionException("Only pending or paid orders can be Cancelled."))
                .when(orderService).cancelOrderAndReleaseStock(1L);

        mockMvc.perform(put("/api/order/1/cancel").with(user("shopper").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ── PUT /api/order/{orderId}/changeShippingAddress ───────────────────

    @Test
    void changeShippingAddress_validRequest_returns200() throws Exception {
        when(orderService.changeShippingAddress(eq(1L), any()))
                .thenReturn(buildChangeAddressResponse(1L, "9876543210"));

        mockMvc.perform(put("/api/order/1/changeShippingAddress")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressLine": "456 New Ave",
                                  "pinCode": "110002",
                                  "contactNumber": "9876543210"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine").value("456 New Ave"))
                .andExpect(jsonPath("$.pinCode").value("110002"))
                .andExpect(jsonPath("$.contactNumber").value("9876543210"));
    }

    @Test
    void changeShippingAddress_missingContactNumber_returns400() throws Exception {
        mockMvc.perform(put("/api/order/1/changeShippingAddress")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressLine": "456 New Ave",
                                  "pinCode": "110002"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsStringIgnoringCase("contact")));

        verifyNoInteractions(orderService);
    }

    @Test
    void changeShippingAddress_invalidContactNumber_returns400() throws Exception {
        mockMvc.perform(put("/api/order/1/changeShippingAddress")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressLine": "456 New Ave",
                                  "pinCode": "110002",
                                  "contactNumber": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsStringIgnoringCase("phone")));

        verifyNoInteractions(orderService);
    }

    @Test
    void changeShippingAddress_notFound_returns404() throws Exception {
        when(orderService.changeShippingAddress(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Order not found: 99"));

        mockMvc.perform(put("/api/order/99/changeShippingAddress")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressLine": "456 New Ave",
                                  "pinCode": "110002",
                                  "contactNumber": "9876543210"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void changeShippingAddress_notOwner_returns403() throws Exception {
        when(orderService.changeShippingAddress(eq(1L), any()))
                .thenThrow(new UnauthorizedAccessException("Unauthorized access"));

        mockMvc.perform(put("/api/order/1/changeShippingAddress")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressLine": "456 New Ave",
                                  "pinCode": "110002",
                                  "contactNumber": "9876543210"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
