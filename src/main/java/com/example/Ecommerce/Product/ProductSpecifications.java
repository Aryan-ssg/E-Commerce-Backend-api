package com.example.Ecommerce.Product;

import org.springframework.data.jpa.domain.Specification;

public class ProductSpecifications {

    private ProductSpecifications() {
       
    }

    public static Specification<Product> hasNameLike(String keyword) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("category").get("categoryId"), categoryId);
                
    }

    public static Specification<Product> hasMinPrice(Integer minPrice) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("productPrice"), minPrice);
    }

    public static Specification<Product> hasMaxPrice(Integer maxPrice) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("productPrice"), maxPrice);
    }

    public static Specification<Product> inStockOnly() {
        return (root, query, cb) ->
                cb.greaterThan(root.get("stock"), 0);
    }
}