package com.example.Ecommerce.Category;

import java.util.List;


public interface CategoryService {

    List<Category> getAllCategories();
    Category createCategories(Category category);
    String deleteCategories(Long id);
    Category updateCategories(Category category, Long categoryId);

}
