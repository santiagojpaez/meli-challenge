package com.challenge.dto;

import java.util.List;

public record ComparisonDiffDTO(
        List<ProductSummaryDTO> products,
        List<ComparisonGroupDTO> attributeGroups
) {}
