package com.example.Ecommerce.Product;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.example.Ecommerce.Common.DTOs.PagedResponse;
import com.example.Ecommerce.Product.DTOs.request.ProductRequest;
import com.example.Ecommerce.Product.DTOs.request.UpdateProductRequest;
import com.example.Ecommerce.Product.DTOs.response.ProductAdminResponse;
import com.example.Ecommerce.Product.DTOs.response.ProductResponse;

public interface ProductService {
    PagedResponse<ProductResponse> getAllProducts(
            String name, Long categoryId, Integer minPrice, Integer maxPrice, Boolean inStock, Pageable pageable);

    ProductResponse getProductById(Long productId);

    ProductAdminResponse createProduct(Long categoryId, ProductRequest product);

    ProductAdminResponse updateProduct(Long productId, UpdateProductRequest updatedProduct);

    void deleteProduct(Long productId);

    ProductAdminResponse uploadImage(Long productId, MultipartFile file);

}
