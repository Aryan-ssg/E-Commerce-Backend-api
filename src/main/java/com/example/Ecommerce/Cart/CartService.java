package com.example.Ecommerce.Cart;

import com.example.Ecommerce.Cart.DTOs.request.AddToCartRequest;
import com.example.Ecommerce.Cart.DTOs.request.UpdateCartItemRequest;
import com.example.Ecommerce.Cart.DTOs.response.CartResponse;

public interface CartService {

    CartResponse getCart();

    CartResponse addToCart(AddToCartRequest request);

    CartResponse updateCartItem(Long productId, UpdateCartItemRequest request);

    CartResponse removeCartItem(Long productId);

    void clearCart();
}