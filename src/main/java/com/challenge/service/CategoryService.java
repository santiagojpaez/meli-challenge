package com.challenge.service;

import com.challenge.dto.AttributeDefinitionSummaryDTO;
import com.challenge.dto.AttributeGroupDTO;
import com.challenge.dto.AttributeRuleSummaryDTO;
import com.challenge.dto.CategoryDetailDTO;
import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.CategoryTreeDTO;
import com.challenge.exception.ItemNotFoundException;
import com.challenge.mapper.CategoryMapper;
import com.challenge.model.Category;
import com.challenge.model.CategoryAttributeRule;
import com.challenge.model.ComparableCategory;
import com.challenge.repository.CategoryAttributeRuleRepository;
import com.challenge.repository.CategoryRepository;
import com.challenge.repository.ComparableCategoryRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRuleRepository ruleRepository;
    private final ComparableCategoryRepository comparableCategoryRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           CategoryAttributeRuleRepository ruleRepository,
                           ComparableCategoryRepository comparableCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.ruleRepository = ruleRepository;
        this.comparableCategoryRepository = comparableCategoryRepository;
    }

    @Cacheable("categoryTree")
    public List<CategoryTreeDTO> getTree() {
        List<Category> all = categoryRepository.findAllWithChildren();
        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(CategoryMapper::toTreeDTO)
                .toList();
    }

    @Cacheable(value = "categoryDetail", key = "#id")
    public CategoryDetailDTO getDetail(Long id) {
        Category category = categoryRepository.findByIdWithParent(id)
                .orElseThrow(() -> ItemNotFoundException.forCategory(id.toString()));

        List<CategorySummaryDTO> comparableWith = getComparableCategories(id);
        List<AttributeGroupDTO> attributeGroups = getAttributeGroups(id);

        return CategoryMapper.toDetailDTO(category, comparableWith, attributeGroups);
    }

    @Cacheable(value = "categoryAttributes", key = "#categoryId")
    public List<AttributeGroupDTO> getAttributeGroups(Long categoryId) {
        List<CategoryAttributeRule> rules = ruleRepository.findByCategoryId(categoryId);
        return buildAttributeGroupDTOs(rules);
    }

    public List<CategorySummaryDTO> getComparableCategories(Long categoryId) {
        return comparableCategoryRepository.findAllByCategoryId(categoryId).stream()
                .map(cc -> {
                    Category other = cc.getCategoryA().getId().equals(categoryId)
                            ? cc.getCategoryB()
                            : cc.getCategoryA();
                    return CategoryMapper.toSummaryDTO(other);
                })
                .toList();
    }

    private List<AttributeGroupDTO> buildAttributeGroupDTOs(List<CategoryAttributeRule> rules) {
        Map<Long, List<CategoryAttributeRule>> byGroup = rules.stream()
                .collect(Collectors.groupingBy(r -> r.getAttributeGroup().getId()));

        return byGroup.entrySet().stream()
                .map(entry -> {
                    List<CategoryAttributeRule> groupRules = entry.getValue();
                    CategoryAttributeRule first = groupRules.getFirst();

                    int groupOrder = groupRules.stream()
                            .mapToInt(CategoryAttributeRule::getDisplayOrder)
                            .min().orElse(0);

                    List<AttributeRuleSummaryDTO> attributes = groupRules.stream()
                            .sorted(Comparator.comparingInt(CategoryAttributeRule::getDisplayOrder))
                            .map(this::toRuleSummary)
                            .toList();

                    return new AttributeGroupDTO(
                            first.getAttributeGroup().getId(),
                            first.getAttributeGroup().getName(),
                            groupOrder,
                            attributes
                    );
                })
                .sorted(Comparator.comparingInt(AttributeGroupDTO::displayOrder))
                .toList();
    }

    private AttributeRuleSummaryDTO toRuleSummary(CategoryAttributeRule rule) {
        var def = rule.getAttributeDefinition();
        return new AttributeRuleSummaryDTO(
                def.getCanonicalName(),
                def.getDisplayName(),
                def.getDataType().name(),
                rule.getIsRequired(),
                rule.getIsComparable(),
                rule.getDisplayOrder(),
                new AttributeDefinitionSummaryDTO(
                        def.getDisplayName(),
                        def.getDescription(),
                        def.getDataType(),
                        def.getComparisonStrategy()
                )
        );
    }
}
