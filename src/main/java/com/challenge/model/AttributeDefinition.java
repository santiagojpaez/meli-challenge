package com.challenge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "attribute_definitions", indexes = {
        // canonical_name ya tiene índice implícito por @Column(unique = true)
        // Índice para findByProductFieldIsNotNull() (resolución de atributos virtuales)
        @Index(name = "idx_attr_def_product_field", columnList = "product_field")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Canonical name is required")
    @Column(name = "canonical_name", nullable = false, unique = true, length = 100)
    private String canonicalName;

    @NotBlank(message = "Display name is required")
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "attributeDefinition", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CategoryAttributeRule> categoryAttributeRules = new ArrayList<>();

    @NotNull(message = "Comparison strategy is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_strategy", nullable = false, length = 20)
    private ComparisonStrategy comparisonStrategy;

    @NotNull(message = "Data type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private AttributeDataType dataType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_group_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UnitGroup unitGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_field", nullable = true, length = 30)
    private ProductField productField;
}
