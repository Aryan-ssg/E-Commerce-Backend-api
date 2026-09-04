package com.example.Ecommerce.Order;

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import com.example.Ecommerce.Order.DTOs.request.OrderItemsRequest;
import com.example.Ecommerce.Order.DTOs.request.PlaceOrderRequest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private PlaceOrderRequest buildValidRequest() {
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

    @Test
    void validRequest_noErrors() {
        PlaceOrderRequest request = buildValidRequest();
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void contactNumber_blank_hasNotBlankError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setContactNumber("");
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("contactNumber"));
    }

    @Test
    void contactNumber_null_hasNotBlankError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setContactNumber(null);
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("contactNumber"));
    }

    @Test
    void contactNumber_tooShort_hasPatternError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setContactNumber("123");
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("contactNumber")
                && v.getMessage().contains("phone"));
    }

    @Test
    void contactNumber_tooLong_hasPatternError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setContactNumber("12345678901");
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("contactNumber")
                && v.getMessage().contains("phone"));
    }

    @Test
    void contactNumber_letters_hasPatternError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setContactNumber("abcdefghij");
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("contactNumber")
                && v.getMessage().contains("phone"));
    }

    @Test
    void contactNumber_withSpecialChars_hasPatternError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setContactNumber("98765-43210");
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("contactNumber")
                && v.getMessage().contains("phone"));
    }

    @Test
    void addressLine_blank_hasNotBlankError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setAddressLine("");
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("addressLine"));
    }

    @Test
    void pinCode_invalidFormat_hasPatternError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setPinCode("000000");
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("pinCode")
                && v.getMessage().contains("PIN"));
    }

    @Test
    void orderItems_empty_hasNotEmptyError() {
        PlaceOrderRequest request = buildValidRequest();
        request.setOrderItems(List.of());
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("orderItems"));
    }

    @Test
    void contactNumber_valid10Digits_noErrors() {
        PlaceOrderRequest request = buildValidRequest();
        request.setContactNumber("0123456789");
        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
