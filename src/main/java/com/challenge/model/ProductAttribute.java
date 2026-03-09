package com.challenge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;


@Entity
@Table(name = "product_attributes", indexes = {
        @Index(name = "idx_pa_product", columnList = "product_id"),
        @Index(name = "idx_pa_attribute_def", columnList = "attribute_def_id"),
        // Índice compuesto para findByProductIdInAndAttributeDefIdIn (comparación con focusedAttributeIds)
        @Index(name = "idx_pa_product_attr_def", columnList = "product_id, attribute_def_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_def_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AttributeDefinition attributeDefinition;

    @NotBlank(message = "Raw value is required")
    @Column(name = "raw_value", nullable = false, length = 1000)
    private String rawValue;

    @Column(name = "raw_unit", length = 50)
    private String rawUnit;

    @Column(name = "normalized_value", precision = 19, scale = 10)
    private BigDecimal normalizedValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "normalized_unit_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Unit normalizedUnit;

    @Column(name = "display_value", length = 255)
    private String displayValue;
}
