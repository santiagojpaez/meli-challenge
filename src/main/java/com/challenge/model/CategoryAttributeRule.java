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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "category_attribute_rules", indexes = {
        @Index(name = "idx_car_category", columnList = "category_id"),
        @Index(name = "idx_car_attribute_def", columnList = "attribute_def_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_def_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AttributeDefinition attributeDefinition;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_group_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AttributeGroup attributeGroup;

    @NotNull
    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @NotNull
    @Column(name = "is_comparable", nullable = false)
    @Builder.Default
    private Boolean isComparable = true;

    @NotNull
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
