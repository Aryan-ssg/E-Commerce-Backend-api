package com.example.Ecommerce.Inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.Common.Exceptions.InsufficientStockException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;
import com.example.Ecommerce.Product.DTOs.response.ProductAdminResponse;


@Service
public class StockServiceImpl implements StockService {

    private ProductRepository productRepository;

    public StockServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void reserveStock(Long productId, int quantity) {

        int updatedRows = productRepository.decrementStockIfAvailable(productId, quantity);

        if (updatedRows == 0) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product with productid : " + productId + " not found"));

            throw new InsufficientStockException("Insufficient stock for product " + product.getProductName()
                    + ", Available: " + product.getStock() + ", requested: " + quantity);
        }

    }

    @Override
    @Transactional
    public void releaseStock(Long productId, int quantity) {
        int updatedRows = productRepository.incrementStock(productId, quantity);

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Product with productid : " + productId + " not found");
        }
    }

    @Override
    @Transactional
    public ProductAdminResponse reStock(Long productId, int quantity) {
        int updatedRows = productRepository.incrementStock(productId, quantity);

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Product with productid : " + productId + " not found");
        }

        Product savedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with productid : " + productId + " not found"));

        return new ProductAdminResponse(savedProduct.getProductId(),
                savedProduct.getProductName(),
                savedProduct.getProductPrice(),
                savedProduct.getCategory().getCategoryName(),
                savedProduct.getStock(),
                savedProduct.getImageUrl());
    }

}
