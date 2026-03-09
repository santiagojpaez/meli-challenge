package com.challenge.dto;

import org.springframework.lang.Nullable;

import java.util.List;

public record CategoryDetailDTO(
        Long id,
        String name,
        @Nullable CategorySummaryDTO parent,
        List<CategorySummaryDTO> comparableWith,
        List<AttributeGroupDTO> attributeGroups
) {}
