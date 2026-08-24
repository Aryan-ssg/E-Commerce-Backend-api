package com.example.Ecommerce.Product;

import com.example.Ecommerce.Category.Category;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @NotBlank(message = "Product name is required")
    private String productName;

    @Positive(message = "Price must be positive")
    @NotNull(message = "Price is required")
    private Integer productPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonBackReference
    private Category category;

    @PositiveOrZero(message = "stock cannot be negative")
    @NotNull(message = "Stock is required")
    private Integer stock;

    // Soft-delete flag. Historical orders keep their product FK, so we never hard-delete;
    // the catalog query filters this out via ProductSpecifications.isActive().
    // columnDefinition gives the ALTER a default so existing rows backfill to true
    // (ddl-auto=update would otherwise fail adding a NOT NULL column to a populated table).
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean active = true;

}
