package com.example.Ecommerce.Category;

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
public class CategoryController {

    private CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/public/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.status(HttpStatus.OK).body(categories);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/categories")
    public ResponseEntity<Category> createCategories(@Valid @RequestBody Category category) {

        Category savedCategory=categoryService.createCategories(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<String> deleteCategories(@PathVariable Long categoryId) {
       
            String status = categoryService.deleteCategories(categoryId);

            return ResponseEntity.status(HttpStatus.OK).body(status);
       

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/categories/{categoryId}")
    public ResponseEntity<Category> updateCategories(@Valid @RequestBody Category category, @PathVariable Long categoryId) {
        
            Category savedCategory = categoryService.updateCategories(category, categoryId);
            return ResponseEntity.status(HttpStatus.OK).body(savedCategory);
      

    }

}
