package com.challenge.mapper;

import com.challenge.dto.ComparisonAttributeDTO;
import com.challenge.dto.ComparisonDiffDTO;
import com.challenge.dto.ComparisonDTO;
import com.challenge.dto.ComparisonGroupDTO;
import com.challenge.dto.ProductSummaryDTO;
import com.challenge.model.Product;
import com.challenge.model.ProductAttribute;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


public final class ComparisonMapper {

    private ComparisonMapper() {
    }

    public static ComparisonDTO toComparisonDTO(
            List<Product> products,
            List<MapperInputs.ComparisonGroupInput> groups,
            List<MapperInputs.MissingAttributeInput> missing) {
        List<ProductSummaryDTO> productDtos = products == null
                ? List.of()
                : products.stream()
                        .map(ProductMapper::toSummaryDTO)
                        .collect(Collectors.toList());
        List<ComparisonGroupDTO> groupList = groups == null
                ? List.of()
                : groups.stream()
                        .map(ComparisonMapper::toComparisonGroupDTOFromInput)
                        .collect(Collectors.toList());
        List<ComparisonDTO.MissingAttributeDTO> missingList = missing == null
                ? List.of()
                : missing.stream()
                        .map(ComparisonMapper::toMissingAttributeDTO)
                        .collect(Collectors.toList());
        return new ComparisonDTO(productDtos, groupList, missingList);
    }

    public static ComparisonDiffDTO toDiffDTO(
            List<Product> products,
            List<MapperInputs.ComparisonGroupInput> groups) {
        List<ProductSummaryDTO> productDtos = products == null
                ? List.of()
                : products.stream()
                        .map(ProductMapper::toSummaryDTO)
                        .collect(Collectors.toList());
        List<ComparisonGroupDTO> groupList = groups == null
                ? List.of()
                : groups.stream()
                        .map(ComparisonMapper::toComparisonGroupDTOFromInput)
                        .collect(Collectors.toList());
        return new ComparisonDiffDTO(productDtos, groupList);
    }

    public static ComparisonGroupDTO toComparisonGroupDTOFromInput(
            MapperInputs.ComparisonGroupInput input) {
        if (input == null) {
            return null;
        }
        List<ComparisonAttributeDTO> attrs = input.attributes() == null
                ? List.of()
                : input.attributes().stream()
                        .map(ComparisonMapper::toComparisonAttributeDTO)
                        .collect(Collectors.toList());
        return new ComparisonGroupDTO(input.groupName(), input.groupOrder(), attrs);
    }

    public static ComparisonAttributeDTO toComparisonAttributeDTO(MapperInputs.ComparisonAttributeInput input) {
        if (input == null) {
            return null;
        }
        List<ComparisonAttributeDTO.AttributeValueDTO> values = input.values() == null
                ? List.of()
                : input.values().stream()
                        .map(ComparisonMapper::toAttributeValueDTO)
                        .collect(Collectors.toList());
        ComparisonAttributeDTO.HighlightDTO highlight = input.highlight() != null
                ? toHighlightDTO(input.highlight())
                : null;
        return new ComparisonAttributeDTO(
                input.attributeDefId(),
                input.displayName(),
                values,
                highlight
        );
    }

    public static ComparisonAttributeDTO.AttributeValueDTO toAttributeValueDTO(MapperInputs.AttributeValueInput input) {
        if (input == null) {
            return null;
        }
        return new ComparisonAttributeDTO.AttributeValueDTO(
                input.productId(),
                input.displayValue(),
                input.normalizedValue()
        );
    }

    public static ComparisonAttributeDTO.HighlightDTO toHighlightDTO(MapperInputs.HighlightInput input) {
        if (input == null) {
            return null;
        }
        return new ComparisonAttributeDTO.HighlightDTO(
                input.winnerIds(),
                input.winnerDisplayValue(),
                input.reason()
        );
    }

    public static ComparisonDTO.MissingAttributeDTO toMissingAttributeDTO(MapperInputs.MissingAttributeInput input) {
        if (input == null) {
            return null;
        }
        return new ComparisonDTO.MissingAttributeDTO(
                input.productId(),
                input.productName(),
                input.attributeDisplayName()
        );
    }

    public static ComparisonAttributeDTO.AttributeValueDTO toValueDTO(String productId, ProductAttribute attribute) {
        if (attribute == null) {
            return new ComparisonAttributeDTO.AttributeValueDTO(productId, null, null);
        }
        String displayValue = attribute.getDisplayValue() != null
                ? attribute.getDisplayValue()
                : attribute.getRawValue();
        BigDecimal normalizedValue = attribute.getNormalizedValue();
        return new ComparisonAttributeDTO.AttributeValueDTO(productId, displayValue, normalizedValue);
    }
}
