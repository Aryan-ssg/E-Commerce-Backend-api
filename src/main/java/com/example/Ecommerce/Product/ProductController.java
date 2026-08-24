package com.example.Ecommerce.Product;



import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecommerce.Common.DTOs.PagedResponse;
import com.example.Ecommerce.Product.DTOs.request.ProductRequest;
import com.example.Ecommerce.Product.DTOs.request.UpdateProductRequest;
import com.example.Ecommerce.Product.DTOs.response.ProductAdminResponse;
import com.example.Ecommerce.Product.DTOs.response.ProductResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProductController {

    private ProductService productService;

    public ProductController(ProductServiceImpl productService) {
        this.productService = productService;

    }

    @GetMapping("/public/products")
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @PageableDefault(size = 20, sort = "productId") Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getAllProducts(name, categoryId, minPrice, maxPrice, inStock, pageable));
    }

    @GetMapping("/public/products/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long productId) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getProductById(productId));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/categories/{categoryId}/products")
    public ResponseEntity<ProductAdminResponse> createProduct(@PathVariable Long categoryId,
            @Valid @RequestBody ProductRequest product) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(categoryId, product));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<ProductAdminResponse> updateProduct(@PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest product) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.updateProduct(productId, product));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productId) {

        productService.deleteProduct(productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body("Product with ProductId : " + productId + " successfully deleted");

    }

}
