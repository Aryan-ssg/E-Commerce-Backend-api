package com.example.Ecommerce.Product;

import java.util.List;



import org.springframework.stereotype.Service;


import com.example.Ecommerce.Category.Category;
import com.example.Ecommerce.Category.CategoryRepository;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;

@Service
public class ProductServiceImpl implements ProductService {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with productid : "+productId+" not found"));

        return product;

    }

    @Override
    public List<Product> getProductsByCategoryId(Long categoryId) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category with categoryid : "+categoryId+" not found");
        }
        return productRepository.findByCategory_CategoryId(categoryId);
    }

    @Override
    public Product createProduct(Long categoryId, Product product) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with categoryid : "+categoryId+" not found"));

        product.setCategory(category);

        return productRepository.save(product);

    }

    @Override
    public Product updateProduct(Long productId, Product updatedProduct) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with productid : "+productId+" not found"));

        existingProduct.setProductName(updatedProduct.getProductName());
        existingProduct.setProductPrice(updatedProduct.getProductPrice());
        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with productid : "+productId+" not found"));

        productRepository.delete(product);

    }

}
