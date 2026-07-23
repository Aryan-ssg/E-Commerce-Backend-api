package com.example.Ecommerce.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();

    List<Product> getProductsByCategoryId(Long categoryId);

    Product getProductById(Long productId);

    Product createProduct(Long categoryId, Product product);

    Product updateProduct(Long productId, Product updatedProduct);

    void deleteProduct(Long productId);
}
