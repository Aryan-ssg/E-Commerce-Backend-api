package com.example.Ecommerce.Product;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.Ecommerce.Category.Category;
import com.example.Ecommerce.Category.CategoryRepository;
import com.example.Ecommerce.Common.DTOs.PagedResponse;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Product.DTOs.request.ProductRequest;
import com.example.Ecommerce.Product.DTOs.request.UpdateProductRequest;
import com.example.Ecommerce.Product.DTOs.response.ProductAdminResponse;
import com.example.Ecommerce.Product.DTOs.response.ProductResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Electronics");

        product = new Product();
        product.setProductId(100L);
        product.setProductName("Headphones");
        product.setProductPrice(2000);
        product.setStock(5);
        product.setCategory(category);
    }

    @Test
    void getAllProducts_mapsPageToPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        PagedResponse<ProductResponse> response =
                productService.getAllProducts(null, null, null, null, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getProductId()).isEqualTo(100L);
        assertThat(response.getContent().get(0).getProductName()).isEqualTo("Headphones");
        assertThat(response.getContent().get(0).getProductPrice()).isEqualTo(2000);
        assertThat(response.getContent().get(0).getCategoryName()).isEqualTo("Electronics");
        assertThat(response.getPageNumber()).isZero();
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void getProductById_found_returnsResponse() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(100L);

        assertThat(response.getProductId()).isEqualTo(100L);
        assertThat(response.getCategoryName()).isEqualTo("Electronics");
    }

    @Test
    void getProductById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createProduct_valid_assignsCategoryAndSaves() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest request = new ProductRequest();
        request.setProductName("Headphones");
        request.setProductPrice(2000);
        request.setStock(5);

        ProductAdminResponse response = productService.createProduct(1L, request);

        assertThat(response.getProductId()).isNull();
        assertThat(response.getProductName()).isEqualTo("Headphones");
        assertThat(response.getCategoryName()).isEqualTo("Electronics");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_categoryNotFound_throwsAndDoesNotSave() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest();
        request.setProductName("Headphones");
        request.setProductPrice(2000);
        request.setStock(5);

        assertThatThrownBy(() -> productService.createProduct(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_updatesOnlyNameAndPrice() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest();
        request.setProductName("Wireless Headphones");
        request.setProductPrice(2500);

        ProductAdminResponse response = productService.updateProduct(100L, request);

        assertThat(response.getProductName()).isEqualTo("Wireless Headphones");
        assertThat(response.getProductPrice()).isEqualTo(2500);
        assertThat(product.getStock()).isEqualTo(5);
        assertThat(product.getCategory()).isEqualTo(category);
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_notFound_throwsAndDoesNotSave() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateProductRequest request = new UpdateProductRequest();
        request.setProductName("Ghost");
        request.setProductPrice(1);

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_found_softDeletes() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        productService.deleteProduct(100L);

        // Soft delete: flag flipped, row retained (so historical orders keep their FK)
        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProduct_notFound_throwsAndDoesNotDelete() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).delete(any(Product.class));
    }
}
