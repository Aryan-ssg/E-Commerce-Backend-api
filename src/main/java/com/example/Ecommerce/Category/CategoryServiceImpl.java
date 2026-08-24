package com.example.Ecommerce.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.Category.DTOs.request.CategoryRequest;
import com.example.Ecommerce.Category.DTOs.response.CategoryResponse;
import com.example.Ecommerce.Category.DTOs.response.GetAllCategoriesResponse;
import com.example.Ecommerce.Category.DTOs.response.Productsresponse;
import com.example.Ecommerce.Common.Exceptions.CategoryAlreadyExistsException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;

@Service
public class CategoryServiceImpl implements CategoryService {

    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<GetAllCategoriesResponse> getAllCategories() {

        List<Category> categories = categoryRepository.findAllWithProducts();

        List<GetAllCategoriesResponse> response = new ArrayList<>();

        for (Category category : categories) {

            List<Productsresponse> products = new ArrayList<>();
            for (Product product : category.getProducts()) {
                Productsresponse productresponse = new Productsresponse(product.getProductId(),
                        product.getProductName(),
                        product.getProductPrice());

                products.add(productresponse);

            }
            GetAllCategoriesResponse currCategory = new GetAllCategoriesResponse(category.getCategoryId(),
                    category.getCategoryName(),
                    products);

            response.add(currCategory);
        }

        return response;

    }

    @Transactional
    @Override
    public CategoryResponse createCategories(CategoryRequest category) {

        if (categoryRepository.findByCategoryName(category.getCategoryName()).isPresent()) {
            throw new CategoryAlreadyExistsException(category.getCategoryName());
        }
        Category requestCategory = new Category();
        requestCategory.setCategoryName(category.getCategoryName());

        Category savedCategory=categoryRepository.save(requestCategory);

        CategoryResponse response=new CategoryResponse(savedCategory.getCategoryId(),savedCategory.getCategoryName());


        return response;

    }

    @Transactional
    @Override
    public String deleteCategories(Long categoryId) {

        Optional<Category> optionalSavedCategory = categoryRepository.findById(categoryId);

        Category savedCategory = optionalSavedCategory
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with category id : " + categoryId + " not found"));

        // Block deletion while products still belong to the category. The category has a
        // CascadeType.ALL / orphanRemoval mapping to its products, so deleting it outright
        // would cascade-delete (or 500 on an FK from historical orders) - we refuse instead.
        if (productRepository.countByCategory_CategoryId(categoryId) > 0) {
            throw new IllegalStateException("Cannot delete category that still has products");
        }

        categoryRepository.delete(savedCategory);
        return "Category removed Successfully";

    }

    @Override
    @Transactional
    public CategoryResponse updateCategories(CategoryRequest category, Long categoryId) {

        Category savedCategory = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException(
                        "Category with category id : " + categoryId + " not found"));

                

        savedCategory.setCategoryName(category.getCategoryName());
        categoryRepository.save(savedCategory);

        CategoryResponse response=new CategoryResponse(savedCategory.getCategoryId(),savedCategory.getCategoryName());
     
        return response;
    }

}
