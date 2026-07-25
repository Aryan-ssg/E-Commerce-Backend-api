package com.example.Ecommerce.Category;


import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.Ecommerce.Common.Exceptions.CategoryAlreadyExistsException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;

@Service
public class CategoryServiceImpl implements CategoryService {
   

    private CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> getAllCategories() {

        return categoryRepository.findAll();
    }

    @Override
    public Category createCategories(Category category) {

        if(categoryRepository.findByCategoryName(category.getCategoryName()).isPresent()){
            throw new CategoryAlreadyExistsException(category.getCategoryName());
        }

        return categoryRepository.save(category);

    }

    @Override
    public String deleteCategories(Long categoryId) {

        Optional<Category> optionalSavedCategory = categoryRepository.findById(categoryId);

        Category savedCategory = optionalSavedCategory
                .orElseThrow(() -> new ResourceNotFoundException("Category with category id : "+categoryId + " not found"));
        categoryRepository.delete(savedCategory);
        return "Category removed Successfully";

    }

    @Override
    public Category updateCategories(Category category, Long categoryId) {

        Optional<Category> optionalSavedCategory = categoryRepository.findById(categoryId);

        Category savedCategory = optionalSavedCategory
                .orElseThrow(() -> new ResourceNotFoundException("Category with category id : "+categoryId + " not found"));

        savedCategory.setCategoryName(category.getCategoryName());

        Category updatedCategory = categoryRepository.save(savedCategory);
        return updatedCategory;
    }

}
