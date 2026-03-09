package com.challenge.service;

import com.challenge.dto.ProductDetailDTO;
import com.challenge.dto.ProductSummaryDTO;
import com.challenge.exception.ItemNotFoundException;
import com.challenge.mapper.MapperInputs;
import com.challenge.mapper.ProductMapper;
import com.challenge.model.CategoryAttributeRule;
import com.challenge.model.Product;
import com.challenge.model.ProductAttribute;
import com.challenge.repository.AttributeDefinitionRepository;
import com.challenge.repository.CategoryAttributeRuleRepository;
import com.challenge.repository.ProductAttributeRepository;
import com.challenge.repository.ProductRepository;
import com.challenge.repository.CategoryRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final CategoryAttributeRuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          ProductAttributeRepository productAttributeRepository,
                          CategoryAttributeRuleRepository ruleRepository,
                          AttributeDefinitionRepository attributeDefinitionRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.ruleRepository = ruleRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<ProductSummaryDTO> listByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategory_Id(categoryId, pageable)
                .map(ProductMapper::toSummaryDTO);
    }

    public Page<ProductSummaryDTO> search(Long categoryId, String query, Pageable pageable) {
        List<Long> categoryIds = categoryRepository.findDescendantIds(categoryId);
        return productRepository.searchByText(categoryIds, query, pageable)
                .map(ProductMapper::toSummaryDTO);
    }

    public ProductDetailDTO getDetail(String productId) {
        Product product = productRepository.findWithRelationsById(productId)
                .orElseThrow(() -> ItemNotFoundException.forProduct(productId));

        List<ProductAttribute> attributes = productAttributeRepository.findByProductId(productId);
        List<CategoryAttributeRule> rules = ruleRepository.findByCategoryId(product.getCategory().getId());

        List<MapperInputs.AttributeGroupValueInput> groupInputs = buildAttributeGroupInputs(rules, attributes);
        return ProductMapper.toDetailDTO(product, groupInputs);
    }

    private List<MapperInputs.AttributeGroupValueInput> buildAttributeGroupInputs(
            List<CategoryAttributeRule> rules,
            List<ProductAttribute> attributes) {

        Map<Long, ProductAttribute> attrByDefId = attributes.stream()
                .collect(Collectors.toMap(
                        pa -> pa.getAttributeDefinition().getId(),
                        pa -> pa,
                        (a, b) -> a
                ));

        Map<Long, List<CategoryAttributeRule>> rulesByGroup = rules.stream()
                .collect(Collectors.groupingBy(r -> r.getAttributeGroup().getId()));

        return rulesByGroup.entrySet().stream()
                .map(entry -> {
                    List<CategoryAttributeRule> groupRules = entry.getValue();
                    CategoryAttributeRule first = groupRules.getFirst();

                    List<MapperInputs.AttributeGroupValueInput.AttributeValueDisplayInput> attrs = groupRules.stream()
                            .sorted(Comparator.comparingInt(CategoryAttributeRule::getDisplayOrder))
                            .map(rule -> {
                                ProductAttribute pa = attrByDefId.get(rule.getAttributeDefinition().getId());
                                String displayValue = pa != null ? pa.getDisplayValue() : null;
                                return new MapperInputs.AttributeGroupValueInput.AttributeValueDisplayInput(
                                        rule.getAttributeDefinition().getDisplayName(),
                                        displayValue
                                );
                            })
                            .toList();

                    return new MapperInputs.AttributeGroupValueInput(
                            first.getAttributeGroup().getId(),
                            first.getAttributeGroup().getName(),
                            attrs
                    );
                })
                .toList();
    }
}
