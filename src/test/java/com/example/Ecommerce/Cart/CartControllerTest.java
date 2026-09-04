package com.example.Ecommerce.Cart;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.Ecommerce.AppUser.AppUserRepository;
import com.example.Ecommerce.Common.JwtUtils;
import com.example.Ecommerce.Common.JwtAuthenticationEntryPoint;
import com.example.Ecommerce.Common.RefreshTokenService;
import com.example.Ecommerce.Common.SecurityConfig;
import com.example.Ecommerce.Cart.DTOs.request.AddToCartRequest;
import com.example.Ecommerce.Cart.DTOs.response.CartItemResponse;
import com.example.Ecommerce.Cart.DTOs.response.CartResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

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

    private CartItemResponse item(Long cartItemId, Long productId, String name, int quantity, int unitPrice) {
        return new CartItemResponse(cartItemId, productId, name, quantity, unitPrice, unitPrice * quantity);
    }

    @Test
    void getCart_authenticatedUser_returns200WithCart() throws Exception {
        CartResponse cartResponse = new CartResponse(1L,
                List.of(item(10L, 100L, "Headphones", 2, 2000)), 4000, false);
        when(cartService.getCart()).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart").with(user("shopper").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(4000))
                .andExpect(jsonPath("$.items", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(100))
                .andExpect(jsonPath("$.items[0].lineTotal").value(4000));
    }

    @Test
    void getCart_adminRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/cart").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    void addToCart_validRequest_returns201AndPassesBody() throws Exception {
        when(cartService.addToCart(any(AddToCartRequest.class)))
                .thenReturn(new CartResponse(1L, List.of(item(10L, 100L, "Headphones", 2, 2000)), 4000, false));

        mockMvc.perform(post("/api/cart/items")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":100,\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        ArgumentCaptor<AddToCartRequest> captor = ArgumentCaptor.forClass(AddToCartRequest.class);
        verify(cartService).addToCart(captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo(100L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void addToCart_zeroQuantity_returns400WithValidationMessage() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":100,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", Matchers.containsString("quantity")));

        verifyNoInteractions(cartService);
    }

    @Test
    void updateCartItem_returns200AndUpdatesQuantity() throws Exception {
        when(cartService.updateCartItem(eq(100L), any()))
                .thenReturn(new CartResponse(1L, List.of(item(10L, 100L, "Headphones", 4, 2000)), 8000, false));

        mockMvc.perform(put("/api/cart/items/100")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(8000));

        verify(cartService).updateCartItem(eq(100L), any());
    }

    @Test
    void removeCartItem_returns200AndRemovesItem() throws Exception {
        when(cartService.removeCartItem(100L))
                .thenReturn(new CartResponse(1L, List.of(), 0, false));

        mockMvc.perform(delete("/api/cart/items/100").with(user("shopper").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", Matchers.hasSize(0)));

        verify(cartService).removeCartItem(100L);
    }

    @Test
    void clearCart_returns200WithMessage() throws Exception {
        mockMvc.perform(delete("/api/cart").with(user("shopper").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string("Cart has been cleared"));

        verify(cartService).clearCart();
    }
}
