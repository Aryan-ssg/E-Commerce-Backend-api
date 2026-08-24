package com.example.Ecommerce.Category;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.Ecommerce.Category.DTOs.request.CategoryRequest;
import com.example.Ecommerce.Category.DTOs.response.CategoryResponse;
import com.example.Ecommerce.Category.DTOs.response.GetAllCategoriesResponse;
import com.example.Ecommerce.Common.Exceptions.CategoryAlreadyExistsException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

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
    void getAllCategories_mapsCategoriesWithTheirProducts() {
        category.setProducts(List.of(product));
        when(categoryRepository.findAllWithProducts()).thenReturn(List.of(category));

        List<GetAllCategoriesResponse> response = categoryService.getAllCategories();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getCategoryId()).isEqualTo(1L);
        assertThat(response.get(0).getCategoryName()).isEqualTo("Electronics");
        assertThat(response.get(0).getProducts()).hasSize(1);
        assertThat(response.get(0).getProducts().get(0).getProductId()).isEqualTo(100L);
        assertThat(response.get(0).getProducts().get(0).getProductPrice()).isEqualTo(2000);
    }

    @Test
    void createCategories_uniqueName_savesAndReturnsResponse() {
        when(categoryRepository.findByCategoryName("Electronics")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Electronics");

        CategoryResponse response = categoryService.createCategories(request);

        assertThat(response.getCategoryName()).isEqualTo("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategories_duplicateName_throwsCategoryAlreadyExistsException() {
        when(categoryRepository.findByCategoryName("Electronics")).thenReturn(Optional.of(category));

        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Electronics");

        assertThatThrownBy(() -> categoryService.createCategories(request))
                .isInstanceOf(CategoryAlreadyExistsException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategories_updatesName() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Home Appliances");

        CategoryResponse response = categoryService.updateCategories(request, 1L);

        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Home Appliances");
        assertThat(category.getCategoryName()).isEqualTo("Home Appliances");
    }

    @Test
    void updateCategories_notFound_throwsAndDoesNotSave() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        CategoryRequest request = new CategoryRequest();
        request.setCategoryName("Home Appliances");

        assertThatThrownBy(() -> categoryService.updateCategories(request, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategories_emptyCategory_deletesAndReturnsMessage() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory_CategoryId(1L)).thenReturn(0L);

        String message = categoryService.deleteCategories(1L);

        assertThat(message).isEqualTo("Category removed Successfully");
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategories_withProducts_throwsAndDoesNotDelete() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory_CategoryId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> categoryService.deleteCategories(1L))
                .isInstanceOf(IllegalStateException.class);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategories_notFound_throwsAndDoesNotDelete() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategories(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }
}
