package com.challenge.mapper;

import com.challenge.dto.AttributeGroupDTO;
import com.challenge.dto.CategoryDetailDTO;
import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.CategoryTreeDTO;
import com.challenge.model.Category;

import java.util.List;
import java.util.stream.Collectors;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryTreeDTO toTreeDTO(Category category) {
        if (category == null) {
            return null;
        }
        List<CategoryTreeDTO> children = category.getChildren() == null
                ? List.of()
                : category.getChildren().stream()
                        .map(CategoryMapper::toTreeDTO)
                        .collect(Collectors.toList());
        return new CategoryTreeDTO(
                category.getId(),
                category.getName(),
                children
        );
    }

    public static CategorySummaryDTO toSummaryDTO(Category category) {
        if (category == null) {
            return null;
        }
        return new CategorySummaryDTO(category.getId(), category.getName());
    }

    public static CategoryDetailDTO toDetailDTO(
            Category category,
            List<CategorySummaryDTO> comparableWith,
            List<AttributeGroupDTO> attributeGroups) {
        if (category == null) {
            return null;
        }
        return new CategoryDetailDTO(
                category.getId(),
                category.getName(),
                toSummaryDTO(category.getParent()),
                comparableWith == null ? List.of() : comparableWith,
                attributeGroups == null ? List.of() : attributeGroups
        );
    }
}
