package com.example.Ecommerce.Product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long productId);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity " +
            "WHERE p.productId = :productId AND p.stock >= :quantity")
    int decrementStockIfAvailable(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.productId = :productId")
    int incrementStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    long countByCategory_CategoryId(@Param("categoryId") Long categoryId);

}
