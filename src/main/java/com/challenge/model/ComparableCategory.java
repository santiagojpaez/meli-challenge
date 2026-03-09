package com.challenge.model;

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
@Table(name = "comparable_categories", indexes = {
        @Index(name = "idx_cc_category_a", columnList = "category_id_a"),
        @Index(name = "idx_cc_category_b", columnList = "category_id_b"),
        @Index(name = "idx_cc_pair", columnList = "category_id_a, category_id_b", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparableCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id_a", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category categoryA;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id_b", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category categoryB;
}
