package com.example.Ecommerce.Cart;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.Cart.DTOs.request.AddToCartRequest;
import com.example.Ecommerce.Cart.DTOs.request.UpdateCartItemRequest;
import com.example.Ecommerce.Cart.DTOs.response.CartResponse;
import com.example.Ecommerce.Category.Category;
import com.example.Ecommerce.Common.AuthenticationHelper;
import com.example.Ecommerce.Common.Exceptions.InsufficientStockException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuthenticationHelper authenticationHelper;

    @InjectMocks
    private CartServiceImpl cartService;

    private AppUser user;
    private Category category;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setUserId(1L);
        user.setUsername("shopper");

        category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Electronics");

        product = new Product();
        product.setProductId(100L);
        product.setProductName("Headphones");
        product.setProductPrice(2000);
        product.setStock(5);
        product.setCategory(category);

        cart = new Cart();
        cart.setCartId(1L);
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());

        when(authenticationHelper.getCurrentUser()).thenReturn(user);
    }

    private CartItem buildItem(Product itemProduct, int quantity) {
        CartItem item = new CartItem();
        item.setCartItemId(itemProduct.getProductId());
        item.setCart(cart);
        item.setProduct(itemProduct);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void getCart_noExistingCart_createsEmptyCart() {
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.getCart();

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalPrice()).isZero();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void getCart_existingCart_returnsItemsWithTotals() {
        cart.getCartItems().add(buildItem(product, 2));
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart();

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalPrice()).isEqualTo(4000);
    }

    @Test
    void addToCart_newProduct_addsSingleItem() {
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(100L);
        request.setQuantity(2);

        CartResponse response = cartService.addToCart(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(0).getLineTotal()).isEqualTo(4000);
        assertThat(response.getTotalPrice()).isEqualTo(4000);
    }

    @Test
    void addToCart_existingProduct_mergesQuantities() {
        cart.getCartItems().add(buildItem(product, 2));
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(100L);
        request.setQuantity(3);

        CartResponse response = cartService.addToCart(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(response.getTotalPrice()).isEqualTo(10000);
    }

    @Test
    void addToCart_productNotFound_throwsAndDoesNotSave() {
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(99L);
        request.setQuantity(1);

        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_updatesQuantity() {
        cart.getCartItems().add(buildItem(product, 2));
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(4);

        CartResponse response = cartService.updateCartItem(100L, request);

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(4);
        assertThat(response.getTotalPrice()).isEqualTo(8000);
    }

    @Test
    void updateCartItem_quantityExceedsStock_throwsAndDoesNotSave() {
        cart.getCartItems().add(buildItem(product, 2));
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(6);

        assertThatThrownBy(() -> cartService.updateCartItem(100L, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Available: 5");

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_quantityExceedsStock_throwsAndDoesNotSave() {
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(100L);
        request.setQuantity(6);

        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Available: 5");

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addToCart_existingItem_mergedQuantityExceedsStock_throws() {
        cart.getCartItems().add(buildItem(product, 3));
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(100L);
        request.setQuantity(3);

        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested: 6");

        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_productNotFound_throws() {
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(4);

        assertThatThrownBy(() -> cartService.updateCartItem(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeCartItem_removesOnlyThatItem() {
        Product otherProduct = new Product();
        otherProduct.setProductId(101L);
        otherProduct.setProductName("Keyboard");
        otherProduct.setProductPrice(500);
        otherProduct.setStock(10);

        cart.getCartItems().add(buildItem(product, 2));
        cart.getCartItems().add(buildItem(otherProduct, 1));
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.removeCartItem(100L);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(101L);
        assertThat(response.getTotalPrice()).isEqualTo(500);
    }

    @Test
    void removeCartItem_productNotInCart_throws() {
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeCartItem(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void clearCart_removesAllItems() {
        cart.getCartItems().add(buildItem(product, 2));
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        cartService.clearCart();

        assertThat(cart.getCartItems()).isEmpty();
        verify(cartRepository).save(cart);
    }
}
