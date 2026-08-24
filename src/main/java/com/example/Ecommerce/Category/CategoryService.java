package com.example.Ecommerce.Category;

import java.util.List;

import com.example.Ecommerce.Category.DTOs.request.CategoryRequest;
import com.example.Ecommerce.Category.DTOs.response.CategoryResponse;
import com.example.Ecommerce.Category.DTOs.response.GetAllCategoriesResponse;


public interface CategoryService {

    List<GetAllCategoriesResponse> getAllCategories();
    CategoryResponse createCategories(CategoryRequest category);
    String deleteCategories(Long id);
    CategoryResponse updateCategories(CategoryRequest category, Long categoryId);

}
