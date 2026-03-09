package com.challenge.dto;

import java.math.BigDecimal;

import org.springframework.lang.Nullable;

import java.util.List;

public record ComparisonAttributeDTO(
        Long attributeDefId,
        String displayName,
        List<AttributeValueDTO> values,
        @Nullable HighlightDTO highlight
) {

        public record AttributeValueDTO(
        String productId,
        @Nullable String displayValue,
        @Nullable BigDecimal normalizedValue
        ) {}

        public record HighlightDTO(
        List<String> winnerIds,
        String winnerDisplayValue,
        String reason
        ) {}
}
