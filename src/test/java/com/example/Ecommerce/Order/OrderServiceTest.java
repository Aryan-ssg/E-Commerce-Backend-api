package com.example.Ecommerce.Order;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.AppUser.Role;
import com.example.Ecommerce.Common.AuthenticationHelper;
import com.example.Ecommerce.Common.Exceptions.InvalidTransitionException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Common.Exceptions.UnauthorizedAccessException;
import com.example.Ecommerce.Order.DTOs.request.ChangeShippingAddressRequest;
import com.example.Ecommerce.Order.DTOs.request.PlaceOrderRequest;
import com.example.Ecommerce.Order.DTOs.request.OrderItemsRequest;
import com.example.Ecommerce.Order.DTOs.request.UpdateOrderStatusRequest;
import com.example.Ecommerce.Order.DTOs.response.ChangeShippingAddressResponse;
import com.example.Ecommerce.Order.DTOs.response.PlaceOrderResponse;
import com.example.Ecommerce.Order.DTOs.response.UpdateOrderStatusResponse;
import com.example.Ecommerce.Order.OrderItem;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Payment.RazorpayService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AuthenticationHelper authenticationHelper;

    @Mock
    private OrderTransactionExecutor orderTransactionExecutor;

    @Mock
    private RazorpayService razorpayService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private AppUser owner;
    private AppUser otherUser;

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setUserId(1L);
        owner.setUsername("owner");
        owner.setRole(Role.USER);

        otherUser = new AppUser();
        otherUser.setUserId(2L);
        otherUser.setUsername("intruder");
        otherUser.setRole(Role.USER);
    }

    private Order buildOrder(Long orderId, AppUser user, OrderStatus status) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUser(user);
        order.setOrderStatus(status);
        order.setAddressLine("123 Old Street");
        order.setPinCode("110001");
        order.setOrderItems(new ArrayList<>());
        return order;
    }




    @Test
    void updateOrderStatus_validTransition_updatesAndSaves() {
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setUpdatedStatus(OrderStatus.PAID);

        UpdateOrderStatusResponse response = orderService.updateOrderStatus(10L, request);

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
        verify(orderTransactionExecutor, never()).cancelOrderTransactional(any(), any());
    }

    @Test
    void updateOrderStatus_invalidTransition_throwsAndDoesNotSave() {
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setUpdatedStatus(OrderStatus.SHIPPED);

        assertThatThrownBy(() -> orderService.updateOrderStatus(10L, request))
                .isInstanceOf(InvalidTransitionException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_toCancelled_delegatesToTransactionExecutor() {
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderTransactionExecutor.cancelOrderTransactional(10L, EnumSet.of(OrderStatus.PENDING, OrderStatus.PAID)))
                .thenReturn(true);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setUpdatedStatus(OrderStatus.CANCELLED);

        orderService.updateOrderStatus(10L, request);

        verify(orderTransactionExecutor).cancelOrderTransactional(10L, EnumSet.of(OrderStatus.PENDING, OrderStatus.PAID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_toCancelled_lostRaceButAlreadyCancelled_isIdempotentSuccess() {
        // First lookup sees PENDING (passes the transition guard), the post-race
        // re-check sees CANCELLED (someone else already cancelled it)
        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(buildOrder(10L, owner, OrderStatus.PENDING)))
                .thenReturn(Optional.of(buildOrder(10L, owner, OrderStatus.CANCELLED)));
        when(orderTransactionExecutor.cancelOrderTransactional(eq(10L), any())).thenReturn(false);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setUpdatedStatus(OrderStatus.CANCELLED);

        assertThatCode(() -> orderService.updateOrderStatus(10L, request)).doesNotThrowAnyException();
    }

    @Test
    void updateOrderStatus_terminalState_alwaysThrows() {
        Order order = buildOrder(10L, owner, OrderStatus.DELIVERED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setUpdatedStatus(OrderStatus.PROCESSING);

        assertThatThrownBy(() -> orderService.updateOrderStatus(10L, request))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void updateOrderStatus_orderNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setUpdatedStatus(OrderStatus.PROCESSING);

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }




    @Test
    void cancelOrder_ownerCancelsPendingOrder_succeeds() {
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);
        when(orderTransactionExecutor.cancelOrderTransactional(10L, EnumSet.of(OrderStatus.PENDING))).thenReturn(true);

        Order result = orderService.cancelOrder(10L);

        assertThat(result).isEqualTo(order);
        verify(orderTransactionExecutor).cancelOrderTransactional(10L, EnumSet.of(OrderStatus.PENDING));
    }

    @Test
    void cancelOrder_nonOwnerAttempts_throwsUnauthorizedAccessException() {
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(authenticationHelper.getCurrentUser()).thenReturn(otherUser);

        assertThatThrownBy(() -> orderService.cancelOrder(10L))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(orderTransactionExecutor, never()).cancelOrderTransactional(any(), any());
    }

    @Test
    void cancelOrder_nonPendingOrder_throwsInvalidTransitionException() {
        Order order = buildOrder(10L, owner, OrderStatus.SHIPPED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);
        // SHIPPED is outside the PENDING-only claim, so the atomic flip loses safely
        when(orderTransactionExecutor.cancelOrderTransactional(eq(10L), any())).thenReturn(false);

        assertThatThrownBy(() -> orderService.cancelOrder(10L))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cancelOrderAndReleaseStock_alreadyCancelled_isIdempotentNoOp() {
        Order order = buildOrder(10L, owner, OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);

        assertThatCode(() -> orderService.cancelOrderAndReleaseStock(10L)).doesNotThrowAnyException();

        verify(orderTransactionExecutor, never()).cancelOrderTransactional(any(), any());
    }

    @Test
    void cancelOrderAndReleaseStock_lostRaceToNonCancellableState_throwsInvalidTransition() {
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);
        // e.g. the admin moved the order to PROCESSING between load and claim
        when(orderTransactionExecutor.cancelOrderTransactional(
                eq(10L), eq(EnumSet.of(OrderStatus.PENDING, OrderStatus.PAID)))).thenReturn(false);

        assertThatThrownBy(() -> orderService.cancelOrderAndReleaseStock(10L))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cancelOrder_orderNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    

    

    @Test
    void changeShippingAddress_ownerOnPendingOrder_updatesAddress() {
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);

        ChangeShippingAddressRequest request = new ChangeShippingAddressRequest();
        request.setAddressLine("456 New Avenue");
        request.setPinCode("110002");

        ChangeShippingAddressResponse response = orderService.changeShippingAddress(10L, request);

        assertThat(response.getAddressLine()).isEqualTo("456 New Avenue");
        assertThat(order.getAddressLine()).isEqualTo("456 New Avenue");
        verify(orderRepository).save(order);
    }

    @Test
    void changeShippingAddress_nonOwner_throwsUnauthorizedAccessException() {
        Order order = buildOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(authenticationHelper.getCurrentUser()).thenReturn(otherUser);

        ChangeShippingAddressRequest request = new ChangeShippingAddressRequest();
        request.setAddressLine("456 New Avenue");
        request.setPinCode("110002");

        assertThatThrownBy(() -> orderService.changeShippingAddress(10L, request))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void changeShippingAddress_nonPendingOrder_throwsInvalidTransitionException() {
        Order order = buildOrder(10L, owner, OrderStatus.PROCESSING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);

        ChangeShippingAddressRequest request = new ChangeShippingAddressRequest();
        request.setAddressLine("456 New Avenue");
        request.setPinCode("110002");

        assertThatThrownBy(() -> orderService.changeShippingAddress(10L, request))
                .isInstanceOf(InvalidTransitionException.class);

        verify(orderRepository, never()).save(any());
    }


    // ── placeOrder ────────────────────────────────────────────────────────

    private PlaceOrderRequest buildPlaceRequest() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressLine("123 Main St");
        request.setPinCode("110001");
        request.setContactNumber("9876543210");
        OrderItemsRequest item = new OrderItemsRequest();
        item.setProductId(100L);
        item.setQuantity(2);
        request.setOrderItems(List.of(item));
        return request;
    }

    private Order buildSavedOrder(AppUser user) {
        Product product = new Product();
        product.setProductId(100L);
        product.setProductPrice(2000);
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setPriceAtCheckout(2000);
        Order order = buildOrder(10L, user, OrderStatus.PENDING);
        order.setContactNumber("9876543210");
        order.setOrderItems(new ArrayList<>(List.of(orderItem)));
        order.setTotalPrice(4000);
        return order;
    }

    @Test
    void placeOrder_noRazorpay_delegatesToTransactionExecutor() {
        PlaceOrderRequest request = buildPlaceRequest();
        Order saved = buildSavedOrder(owner);
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);
        when(orderTransactionExecutor.placeOrderTransactional(request, owner)).thenReturn(saved);

        PlaceOrderResponse response = orderService.placeOrder(request);

        assertThat(response.getOrderId()).isEqualTo(10L);
        assertThat(response.getContactNumber()).isEqualTo("9876543210");
        assertThat(response.getAddressLine()).isEqualTo("123 Old Street");
        verify(orderTransactionExecutor).placeOrderTransactional(request, owner);
    }

    @Test
    void placeOrder_withRazorpay_validSignature_delegatesToTransactionExecutor() {
        PlaceOrderRequest request = buildPlaceRequest();
        request.setRazorpayOrderId("order_abc");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("sig_xyz");
        Order saved = buildSavedOrder(owner);
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);
        when(razorpayService.verifyPaymentSignature("order_abc", "pay_123", "sig_xyz")).thenReturn(true);
        when(orderTransactionExecutor.placeOrderTransactional(request, owner)).thenReturn(saved);

        PlaceOrderResponse response = orderService.placeOrder(request);

        assertThat(response.getOrderId()).isEqualTo(10L);
        verify(razorpayService).verifyPaymentSignature("order_abc", "pay_123", "sig_xyz");
        verify(orderTransactionExecutor).placeOrderTransactional(request, owner);
    }

    @Test
    void placeOrder_withRazorpay_invalidSignature_throwsIllegalArgument() {
        PlaceOrderRequest request = buildPlaceRequest();
        request.setRazorpayOrderId("order_abc");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("bad_sig");
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);
        when(razorpayService.verifyPaymentSignature("order_abc", "pay_123", "bad_sig")).thenReturn(false);

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment verification failed");

        verify(orderTransactionExecutor, never()).placeOrderTransactional(any(), any());
    }

    @Test
    void placeOrder_withRazorpay_verifyThrows_throwsPaymentException() {
        PlaceOrderRequest request = buildPlaceRequest();
        request.setRazorpayOrderId("order_abc");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("sig_xyz");
        when(authenticationHelper.getCurrentUser()).thenReturn(owner);
        when(razorpayService.verifyPaymentSignature("order_abc", "pay_123", "sig_xyz"))
                .thenThrow(new RazorpayService.PaymentException("Razorpay down"));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(RazorpayService.PaymentException.class)
                .hasMessageContaining("Razorpay down");

        verify(orderTransactionExecutor, never()).placeOrderTransactional(any(), any());
    }
}