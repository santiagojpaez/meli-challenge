package com.challenge.dto;

import java.util.List;

public record ComparisonDTO(
        List<ProductSummaryDTO> products,
        List<ComparisonGroupDTO> attributeGroups,
        List<MissingAttributeDTO> missingAttributes
) {

        public record MissingAttributeDTO(
        String productId,
        String productName,
        String attributeDisplayName
        ) {}

}
