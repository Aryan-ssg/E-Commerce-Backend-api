package com.example.Ecommerce.Product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.Ecommerce.Category.Category;
import com.example.Ecommerce.Category.CategoryRepository;
import com.example.Ecommerce.Common.DTOs.PagedResponse;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Common.FileStorageService;
import com.example.Ecommerce.Product.DTOs.request.ProductRequest;
import com.example.Ecommerce.Product.DTOs.request.UpdateProductRequest;
import com.example.Ecommerce.Product.DTOs.response.ProductAdminResponse;
import com.example.Ecommerce.Product.DTOs.response.ProductResponse;

@Service
public class ProductServiceImpl implements ProductService {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private FileStorageService fileStorageService;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository,
            FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public PagedResponse<ProductResponse> getAllProducts(
            String name, Long categoryId, Integer minPrice, Integer maxPrice, Boolean inStock, Pageable pageable) {

        Specification<Product> spec = Specification.allOf();

        if (name != null && !name.isBlank()) {
            spec = spec.and(ProductSpecifications.hasNameLike(name));
        }
        if (categoryId != null) {
            spec = spec.and(ProductSpecifications.hasCategoryId(categoryId));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.hasMinPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.hasMaxPrice(maxPrice));
        }
        if (inStock != null && inStock) {
            spec = spec.and(ProductSpecifications.inStockOnly());
        }

        // Always exclude soft-deleted products from the catalog
        spec = spec.and(ProductSpecifications.isActive());

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductResponse> content = new ArrayList<>();
        for (Product product : productPage.getContent()) {
            content.add(new ProductResponse(
                    product.getProductId(),
                    product.getProductName(),
                    product.getProductPrice(),
                    product.getCategory().getCategoryName(),
                    product.getImageUrl(),
                    product.getStock()));
        }

        return new PagedResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast());

    }

    @Override
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product with productid : " + productId + " not found"));

        ProductResponse response = new ProductResponse(product.getProductId(),
                product.getProductName(),
                product.getProductPrice(),
                product.getCategory().getCategoryName(),
                product.getImageUrl(),
                product.getStock());
        return response;

    }

    @Override
    public ProductAdminResponse createProduct(Long categoryId, ProductRequest product) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Category with categoryid : " + categoryId + " not found"));

        Product requestProduct = new Product();
        requestProduct.setCategory(category);
        requestProduct.setProductName(product.getProductName());
        requestProduct.setProductPrice(product.getProductPrice());
        requestProduct.setStock(product.getStock());
        requestProduct.setImageUrl(product.getImageUrl());

        Product savedProduct = productRepository.save(requestProduct);

        ProductAdminResponse response = new ProductAdminResponse(savedProduct.getProductId(),
                savedProduct.getProductName(),
                savedProduct.getProductPrice(),
                category.getCategoryName(),
                savedProduct.getStock(),
                savedProduct.getImageUrl());

        return response;

    }

    @Override
    public ProductAdminResponse updateProduct(Long productId, UpdateProductRequest updatedProduct) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product with productid : " + productId + " not found"));

        existingProduct.setProductName(updatedProduct.getProductName());
        existingProduct.setProductPrice(updatedProduct.getProductPrice());
        existingProduct.setImageUrl(updatedProduct.getImageUrl());

        Product savedProduct = productRepository.save(existingProduct);

        ProductAdminResponse response = new ProductAdminResponse(savedProduct.getProductId(),
                savedProduct.getProductName(),
                savedProduct.getProductPrice(),
                savedProduct.getCategory().getCategoryName(),
                savedProduct.getStock(),
                savedProduct.getImageUrl());

        return response;
    }

    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product with productid : " + productId + " not found"));

        // Soft delete: keep the row so historical orders keep their FK, just hide it
        // from the catalog and prevent further sales.
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public ProductAdminResponse uploadImage(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product with productid : " + productId + " not found"));

        fileStorageService.deleteFile(product.getImageUrl());
        String imageUrl = fileStorageService.storeFile(file);
        product.setImageUrl(imageUrl);

        Product savedProduct = productRepository.save(product);

        return new ProductAdminResponse(savedProduct.getProductId(),
                savedProduct.getProductName(),
                savedProduct.getProductPrice(),
                savedProduct.getCategory().getCategoryName(),
                savedProduct.getStock(),
                savedProduct.getImageUrl());
    }

}
