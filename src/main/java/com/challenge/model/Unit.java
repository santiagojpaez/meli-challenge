package com.challenge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "units", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = {"unit_group_id", "symbol"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_group_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UnitGroup unitGroup;

    @NotBlank(message = "Unit symbol is required")
    @Column(nullable = false, length = 20)
    private String symbol;

    @NotNull(message = "Conversion factor is required")
    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 10)
    private BigDecimal conversionFactor;

    @NotNull
    @Column(name = "is_base_unit", nullable = false)
    @Builder.Default
    private Boolean isBaseUnit = false;
}
