package com.challenge.dto;

import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailDTO(
        ProductSummaryDTO productSummary,
        @Nullable BigDecimal weight,
        @Nullable String size,
        @Nullable Integer availableQuantity,
        @Nullable Integer soldQuantity,
        @Nullable CategorySummaryDTO category,
        List<AttributeGroupValueDTO> attributeGroups
) {}
