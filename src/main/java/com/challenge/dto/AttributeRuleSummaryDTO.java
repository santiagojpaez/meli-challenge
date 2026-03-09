package com.challenge.dto;

public record AttributeRuleSummaryDTO(
        String canonicalName,
        String displayName,
        String dataType,
        boolean isRequired,
        boolean isComparable,
        int displayOrder,
        AttributeDefinitionSummaryDTO attributeDefinition
) {}
