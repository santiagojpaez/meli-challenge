package com.challenge.dto;

import com.challenge.model.AttributeDataType;
import com.challenge.model.ComparisonStrategy;
import org.springframework.lang.Nullable;

public record AttributeDefinitionSummaryDTO(
        String displayName,
        @Nullable String description,
        AttributeDataType dataType,
        ComparisonStrategy comparisonStrategy
) {}
