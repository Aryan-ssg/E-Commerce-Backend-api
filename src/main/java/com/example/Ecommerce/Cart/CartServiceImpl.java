package com.example.Ecommerce.Cart;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.Cart.DTOs.request.AddToCartRequest;
import com.example.Ecommerce.Cart.DTOs.request.UpdateCartItemRequest;
import com.example.Ecommerce.Cart.DTOs.response.CartItemResponse;
import com.example.Ecommerce.Cart.DTOs.response.CartResponse;
import com.example.Ecommerce.Common.AuthenticationHelper;
import com.example.Ecommerce.Common.Exceptions.InsufficientStockException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;

@Service
public class CartServiceImpl implements CartService {

    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private AuthenticationHelper authenticationHelper;

    public CartServiceImpl(CartRepository cartRepository, ProductRepository productRepository,
            AuthenticationHelper authenticationHelper) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.authenticationHelper = authenticationHelper;
    }

    @Override
    @Transactional
    public CartResponse getCart() {
        AppUser currentUser = authenticationHelper.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        AppUser currentUser = authenticationHelper.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with productid : " + request.getProductId() + " not found"));

        CartItem existingItem = findCartItemOrNull(cart, product.getProductId());

        int requestedQuantity = (existingItem != null)
                ? existingItem.getQuantity() + request.getQuantity()
                : request.getQuantity();
        validateAvailableStock(product, requestedQuantity);

        boolean itemAlreadyInCart = existingItem != null;

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cart.getCartItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        CartResponse response = buildCartResponse(savedCart);
        response.setItemAlreadyInCart(itemAlreadyInCart);
        return response;
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long productId, UpdateCartItemRequest request) {
        AppUser currentUser = authenticationHelper.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        CartItem item = findCartItemOrThrow(cart, productId);
        validateAvailableStock(item.getProduct(), request.getQuantity());
        item.setQuantity(request.getQuantity());

        Cart savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long productId) {
        AppUser currentUser = authenticationHelper.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        CartItem item = findCartItemOrThrow(cart, productId);
        cart.getCartItems().remove(item);

        Cart savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    @Override
    @Transactional
    public void clearCart() {
        AppUser currentUser = authenticationHelper.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(AppUser user) {
        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setCartItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });
    }

    private CartItem findCartItemOrNull(Cart cart, Long productId) {
        for (CartItem item : cart.getCartItems()) {
            if (item.getProduct().getProductId().equals(productId)) {
                return item;
            }
        }
        return null;
    }

    private CartItem findCartItemOrThrow(Cart cart, Long productId) {
        CartItem item = findCartItemOrNull(cart, productId);
        if (item == null) {
            throw new ResourceNotFoundException("Product with productid : " + productId + " not found in cart");
        }
        return item;
    }

    private void validateAvailableStock(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getStock()) {
            throw new InsufficientStockException("Insufficient stock for product " + product.getProductName()
                    + ", Available: " + product.getStock() + ", requested: " + requestedQuantity);
        }
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalPrice = 0;

        for (CartItem item : cart.getCartItems()) {
            int unitPrice = item.getProduct().getProductPrice();
            int lineTotal = unitPrice * item.getQuantity();
            totalPrice += lineTotal;

            itemResponses.add(new CartItemResponse(
                    item.getCartItemId(),
                    item.getProduct().getProductId(),
                    item.getProduct().getProductName(),
                    item.getQuantity(),
                    unitPrice,
                    lineTotal));
        }

        return new CartResponse(cart.getCartId(), itemResponses, totalPrice, false);
    }
}