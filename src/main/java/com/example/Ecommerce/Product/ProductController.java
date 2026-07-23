package com.example.Ecommerce.Product;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProductController {

    private ProductService productService;

    public ProductController(ProductServiceImpl productService) {
        this.productService = productService;

    }

    @GetMapping("/public/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getAllProducts());

    }

    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<List<Product>> getProductsByCategoryId(@Valid @PathVariable Long categoryId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getProductsByCategoryId(categoryId));
    }

    @GetMapping("/public/products/{productId}")
    public ResponseEntity<Product> getProductById(@Valid @PathVariable Long productId) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getProductById(productId));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/categories/{categoryId}/products")
    public ResponseEntity<Product> createProduct(@Valid @PathVariable Long categoryId, @RequestBody Product product) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(categoryId, product));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<Product> updateProduct(@Valid @PathVariable Long productId, @RequestBody Product product) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.updateProduct(productId, product));

    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<Product> deleteProduct(@Valid @PathVariable Long productId) {

        productService.deleteProduct(productId);
        return ResponseEntity.status(HttpStatus.OK).build();

    }

}
