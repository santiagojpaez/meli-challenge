package com.challenge.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;

import java.math.BigDecimal;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_condition", columnList = "product_condition"),
        // En producción (PostgreSQL) reemplazar por índice funcional: CREATE INDEX idx_product_name_lower ON products (LOWER(name))
        @Index(name = "idx_product_name", columnList = "name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @Column(length = 20)
    private String id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, length = 500)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Condition is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 15)
    private ItemCondition condition;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(length = 100)
    private String color;

    @Column(precision = 12, scale = 4)
    private BigDecimal weight;

    @Column(length = 100)
    private String size;

    @PositiveOrZero
    private Double rating;

    @PositiveOrZero
    @Column(name = "available_quantity")
    @Builder.Default
    private Integer availableQuantity = 0;

    @PositiveOrZero
    @Column(name = "sold_quantity")
    @Builder.Default
    private Integer soldQuantity = 0;

    // Relaciones

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "price_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Price price;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shipping_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Shipping shipping;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductAttribute> attributes = new ArrayList<>();
}
