package com.example.Ecommerce.Order;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.AppUser.AppUserRepository;
import com.example.Ecommerce.Common.AuthenticationHelper;
import com.example.Ecommerce.Order.DTOs.request.ChangeShippingAddressRequest;
import com.example.Ecommerce.Order.DTOs.request.OrderItemsRequest;
import com.example.Ecommerce.Order.DTOs.request.PlaceOrderRequest;
import com.example.Ecommerce.Order.DTOs.request.UpdateOrderStatusRequest;
import com.example.Ecommerce.Order.DTOs.response.ChangeShippingAddressResponse;
import com.example.Ecommerce.Order.DTOs.response.GetOrderByIdResponse;
import com.example.Ecommerce.Order.DTOs.response.OrderItemsResponse;
import com.example.Ecommerce.Order.DTOs.response.PlaceOrderResponse;
import com.example.Ecommerce.Order.DTOs.response.UpdateOrderStatusResponse;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;

@Service
public class OrderService {

    private OrderRepository orderRepository;
    private AppUserRepository appUserRepository;
    private ProductRepository productRepository;
    private AuthenticationHelper authenticationHelper;

    public OrderService(OrderRepository orderRepository, AppUserRepository appUserRepository,
            ProductRepository productRepository , AuthenticationHelper authenticationHelper) {
        this.orderRepository = orderRepository;
        this.appUserRepository = appUserRepository;
        this.productRepository = productRepository;
        this.authenticationHelper=authenticationHelper;
    }

    public List<GetOrderByIdResponse> getOrdersForCurrentUser(){
        AppUser currentUser=authenticationHelper.getCurrentUser();

        List<Order> orders=orderRepository.findByUser_UserId(currentUser.getUserId());


        //Response

        List<GetOrderByIdResponse> response=new ArrayList<>();


        for(Order order:orders){
            List<OrderItemsResponse> orderItems=new ArrayList<>();

            for(OrderItem item:order.getOrderItems()){
                OrderItemsResponse orderItem=new OrderItemsResponse(
                    item.getQuantity(),
                    item.getProduct().getProductId(),
                    item.getPriceAtCheckout()
                );

                orderItems.add(orderItem);
            }


            GetOrderByIdResponse orderResponse=new GetOrderByIdResponse(
                order.getOrderId(),
                order.getTotalPrice(),
                order.getShippingAddress(),
                order.getOrderDateTime(),
                order.getOrderStatus(),
                orderItems
            
            );

            response.add(orderResponse);

          
        }
        return response;

    }

    public PlaceOrderResponse placeOrder(PlaceOrderRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String userName = auth.getName();

        AppUser user = appUserRepository.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User with username : " + userName + " not found"));

        Order order = new Order();

        order.setUser(user);
        order.setOrderDateTime(LocalDateTime.now());
        order.setShippingAddress(request.getShippingAddress());
        order.setOrderStatus(OrderStatus.PENDING);

        int totalOrderPrice = 0;
        List<OrderItemsResponse> orderItemsResponse = new ArrayList<>();
        List<OrderItem> itemlist = new ArrayList<>();

        for (OrderItemsRequest orderItem : request.getOrderItems()) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product doesnt exist with product id : " + orderItem.getProductId()));

            OrderItem item = new OrderItem();

            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(orderItem.getQuantity());
            item.setPriceAtCheckout(product.getProductPrice());
            orderItemsResponse
                    .add(new OrderItemsResponse(item.getQuantity(), item.getProduct().getProductId(),
                            item.getPriceAtCheckout()));

            totalOrderPrice += product.getProductPrice() * orderItem.getQuantity();
            itemlist.add(item);

        }
        order.setOrderItems(itemlist);
        order.setTotalPrice(totalOrderPrice);

        Order savedOrder = orderRepository.save(order);

        // Response
        PlaceOrderResponse response = new PlaceOrderResponse();

        response.setOrderDateTime(savedOrder.getOrderDateTime());
        response.setShippingAddress(savedOrder.getShippingAddress());
        response.setOrderStatus(savedOrder.getOrderStatus());
        response.setTotalPrice(savedOrder.getTotalPrice());
        response.setOrderItems(orderItemsResponse);
        response.setOrderId(savedOrder.getOrderId());
        return response;

    }

    public ChangeShippingAddressResponse changeShippingAddress(Long orderId, ChangeShippingAddressRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));



        AppUser currentUser = authenticationHelper.getCurrentUser();

        if(!order.getUser().getUserId().equals(currentUser.getUserId())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not allowed to access this order.");
        }
          
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Shipping address can only be changed for pending orders.");
        }

        order.setShippingAddress(request.getNewShippingAddress());
        orderRepository.save(order);

        // Response
        ChangeShippingAddressResponse response = new ChangeShippingAddressResponse(orderId, order.getShippingAddress());

        return response;

    }

    public UpdateOrderStatusResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));


        
        OrderStatus currentStatus = order.getOrderStatus();

        if (!currentStatus.canTransitionTo(request.getUpdatedStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Given transition is prohibited");
        }
        order.setOrderStatus(request.getUpdatedStatus());
        orderRepository.save(order);

        // Response
        UpdateOrderStatusResponse response = new UpdateOrderStatusResponse(orderId, request.getUpdatedStatus());
        return response;

    }

    public GetOrderByIdResponse getOrderByOrderId(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

       

        AppUser currentUser = authenticationHelper.getCurrentUser();

        if(!order.getUser().getUserId().equals(currentUser.getUserId())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not allowed to access this order.");
        }
                
        // Response
        GetOrderByIdResponse response = new GetOrderByIdResponse();

        response.setOrderDateTime(order.getOrderDateTime());
        response.setOrderId(order.getOrderId());
        response.setOrderStatus(order.getOrderStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setTotalPrice(order.getTotalPrice());

        List<OrderItemsResponse> orderItemsResponse = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            OrderItemsResponse orderItemResponse = new OrderItemsResponse();
            orderItemResponse.setPriceAtCheckout(item.getPriceAtCheckout());
            orderItemResponse.setProductId(item.getProduct().getProductId());
            orderItemResponse.setQuantity(item.getQuantity());
            orderItemsResponse.add(orderItemResponse);
        }

        response.setOrderItems(orderItemsResponse);

        return response;
    }

    public Order cancelOrder(Long orderId){
          Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

       
        if(order.getOrderStatus()!=OrderStatus.PENDING){
            throw new NullPointerException("Only Pending orders can be Cancelled.");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);

    }

}
