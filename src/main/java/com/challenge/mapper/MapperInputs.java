package com.challenge.mapper;

import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tipos de entrada para Mappers. El service popula estos con los resultados de
 * sus cálculos.
 */
public final class MapperInputs {

    private MapperInputs() {}

    public record ComparisonGroupInput(
            String groupName,
            int groupOrder,
            List<ComparisonAttributeInput> attributes
        ) {}

    public record ComparisonAttributeInput(
            Long attributeDefId,
            String displayName,
            List<AttributeValueInput> values,
            @Nullable HighlightInput highlight
        ) {}

    public record AttributeValueInput(
            String productId,
            @Nullable String displayValue,
            @Nullable BigDecimal normalizedValue
        ) {}

    public record HighlightInput(
            List<String> winnerIds,
            String winnerDisplayValue,
            String reason
        ) {}

    public record MissingAttributeInput(
            String productId,
            String productName,
            String attributeDisplayName
        ) {}

    public record AttributeGroupValueInput(
            Long groupId,
            String groupName,
            List<AttributeValueDisplayInput> attributes
        ) {

        public record AttributeValueDisplayInput(String displayName, String displayValue) {}
    }
}
