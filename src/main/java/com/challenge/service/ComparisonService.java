package com.challenge.service;

import com.challenge.dto.ComparisonDTO;
import com.challenge.dto.ComparisonDiffDTO;
import com.challenge.dto.ComparisonRequestDTO;
import com.challenge.exception.CategoryMismatchException;
import com.challenge.exception.ItemNotFoundException;
import com.challenge.mapper.ComparisonMapper;
import com.challenge.mapper.MapperInputs;
import com.challenge.model.AttributeDataType;
import com.challenge.model.AttributeDefinition;
import com.challenge.model.CategoryAttributeRule;
import com.challenge.model.ComparisonStrategy;
import com.challenge.model.Product;
import com.challenge.model.ProductAttribute;
import com.challenge.repository.AttributeDefinitionRepository;
import com.challenge.repository.CategoryAttributeRuleRepository;
import com.challenge.repository.ComparableCategoryRepository;
import com.challenge.repository.ProductAttributeRepository;
import com.challenge.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.challenge.exception.InvalidComparisonRequestException;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ComparisonService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final CategoryAttributeRuleRepository ruleRepository;
    private final ComparableCategoryRepository comparableCategoryRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final ProductFieldResolver productFieldResolver;

    public ComparisonService(ProductRepository productRepository,
            ProductAttributeRepository productAttributeRepository,
            CategoryAttributeRuleRepository ruleRepository,
            ComparableCategoryRepository comparableCategoryRepository,
            AttributeDefinitionRepository attributeDefinitionRepository,
            ProductFieldResolver productFieldResolver) {
        this.productRepository = productRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.ruleRepository = ruleRepository;
        this.comparableCategoryRepository = comparableCategoryRepository;
        this.attributeDefinitionRepository = attributeDefinitionRepository;
        this.productFieldResolver = productFieldResolver;
    }

    public ComparisonDTO compare(ComparisonRequestDTO request) {
        List<Product> products = resolveAndValidateProducts(request.productIds());
        Set<Long> categoryIds = extractCategoryIds(products);
        validateCategoriesAreComparable(categoryIds);

        Set<Long> focusedSet = request.focusedAttributeIds() != null
                ? new HashSet<>(request.focusedAttributeIds())
                : Set.of();

        List<AttributeDefinition> virtualDefs = fetchVirtualDefs(focusedSet);
        Set<Long> virtualDefIds = virtualDefs.stream().map(AttributeDefinition::getId).collect(Collectors.toSet());

        List<CategoryAttributeRule> rules = resolveRules(categoryIds, focusedSet, virtualDefIds);
        List<ProductAttribute> allAttributes = fetchAttributes(request, products, virtualDefIds);

        Map<Long, Map<String, ProductAttribute>> attrByDefByProduct = indexAttributes(allAttributes);
        List<MapperInputs.ComparisonGroupInput> groups = new ArrayList<>(buildGroups(rules, products, attrByDefByProduct, focusedSet));

        if (!virtualDefs.isEmpty()) {
            List<MapperInputs.ComparisonAttributeInput> virtualInputs = buildVirtualAttributeInputs(virtualDefs, products, focusedSet);
            if (!virtualInputs.isEmpty()) {
                groups.add(new MapperInputs.ComparisonGroupInput("Precio y Valoración", -1, virtualInputs));
                groups.sort(Comparator.comparingInt(MapperInputs.ComparisonGroupInput::groupOrder));
            }
        }

        List<MapperInputs.MissingAttributeInput> missing = buildMissing(rules, products, attrByDefByProduct);

        return ComparisonMapper.toComparisonDTO(products, groups, missing);
    }

    public ComparisonDiffDTO diff(ComparisonRequestDTO request) {
        List<Product> products = resolveAndValidateProducts(request.productIds());
        Set<Long> categoryIds = extractCategoryIds(products);
        validateCategoriesAreComparable(categoryIds);

        Set<Long> focusedSet = request.focusedAttributeIds() != null
                ? new HashSet<>(request.focusedAttributeIds())
                : Set.of();

        List<AttributeDefinition> virtualDefs = fetchVirtualDefs(focusedSet);
        Set<Long> virtualDefIds = virtualDefs.stream().map(AttributeDefinition::getId).collect(Collectors.toSet());

        List<CategoryAttributeRule> rules = resolveRules(categoryIds, focusedSet, virtualDefIds);
        List<ProductAttribute> allAttributes = fetchAttributes(request, products, virtualDefIds);

        Map<Long, Map<String, ProductAttribute>> attrByDefByProduct = indexAttributes(allAttributes);

        List<MapperInputs.ComparisonGroupInput> diffGroups = buildGroups(rules, products, attrByDefByProduct, focusedSet)
                .stream()
                .map(group -> new MapperInputs.ComparisonGroupInput(
                        group.groupName(),
                        group.groupOrder(),
                        group.attributes().stream()
                                .filter(this::hasDifferences)
                                .toList()
                ))
                .filter(group -> !group.attributes().isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));

        if (!virtualDefs.isEmpty()) {
            List<MapperInputs.ComparisonAttributeInput> virtualDiffAttrs = virtualDefs.stream()
                    .map(def -> buildVirtualAttributeInput(def, products, focusedSet))
                    .filter(this::hasDifferences)
                    .toList();
            if (!virtualDiffAttrs.isEmpty()) {
                diffGroups.add(new MapperInputs.ComparisonGroupInput("Precio y Valoración", -1, virtualDiffAttrs));
                diffGroups.sort(Comparator.comparingInt(MapperInputs.ComparisonGroupInput::groupOrder));
            }
        }

        return ComparisonMapper.toDiffDTO(products, diffGroups);
    }

    // ── Validaciones ──────────────────────────────────────────────────────
    private List<Product> resolveAndValidateProducts(List<String> productIds) {
        Set<String> uniqueIds = new HashSet<>();
        for (String id : productIds) {
            if (!uniqueIds.add(id)) {
                throw InvalidComparisonRequestException.sameProduct();
            }
        }

        List<Product> products = productRepository.findAllByIdIn(productIds);

        if (products.size() != productIds.size()) {
            Set<String> found = products.stream().map(Product::getId).collect(Collectors.toSet());
            String missing = productIds.stream()
                    .filter(id -> !found.contains(id))
                    .findFirst().orElse("unknown");
            throw ItemNotFoundException.forProduct(missing);
        }

        return products;
    }

    private Set<Long> extractCategoryIds(List<Product> products) {
        return products.stream()
                .map(p -> p.getCategory().getId())
                .collect(Collectors.toSet());
    }

    private void validateCategoriesAreComparable(Set<Long> categoryIds) {
        if (categoryIds.size() <= 1) {
            return;
        }

        List<Long> ids = new ArrayList<>(categoryIds);
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                Long idA = ids.get(i);
                Long idB = ids.get(j);
                comparableCategoryRepository.findPairByCategoryIds(idA, idB)
                        .orElseThrow(() -> CategoryMismatchException.forProducts(
                        idA.toString(), idB.toString()));
            }
        }
    }

    // ── Obtención de Datos ───────────────────────────────────────────────────
    private List<ProductAttribute> fetchAttributes(ComparisonRequestDTO request, List<Product> products,
            Set<Long> virtualDefIds) {
        List<String> prodIds = products.stream().map(Product::getId).toList();

        if (request.focusedAttributeIds() != null && !request.focusedAttributeIds().isEmpty()) {
            // Borrar virtual IDs porque no tienen ProductAttributes asociados
            List<Long> realIds = request.focusedAttributeIds().stream()
                    .filter(id -> !virtualDefIds.contains(id))
                    .toList();
            if (realIds.isEmpty()) {
                return List.of();
            }
            List<ProductAttribute> attributes =
                    productAttributeRepository.findByProductIdInAndAttributeDefIdIn(prodIds, realIds);
            if (attributes.isEmpty()) {
                throw new ItemNotFoundException("No se encontraron los atributos enfocados para los productos indicados");
            }
            return attributes;
        }

        List<ProductAttribute> attributes = productAttributeRepository.findByProductIdIn(prodIds);
        if (attributes.isEmpty()) {
            throw new ItemNotFoundException("No se encontraron atributos para los productos indicados");
        }
        return attributes;
    }

    private List<CategoryAttributeRule> resolveRules(Set<Long> categoryIds, Set<Long> focusedSet,
            Set<Long> virtualDefIds) {
        List<CategoryAttributeRule> rules = ruleRepository.findByCategoryIdIn(new ArrayList<>(categoryIds));

        // Mantener solo una regla por AttributeDefinition
        List<CategoryAttributeRule> deduped = rules.stream()
                .collect(Collectors.toMap(
                        r -> r.getAttributeDefinition().getId(),
                        r -> r,
                        (a, b) -> a
                ))
                .values().stream()
                .toList();

        if (focusedSet.isEmpty()) {
            return deduped;
        }

        // Excluir reglas que no estén en focusedSet o que correspondan a virtual defs (no tienen reglas asociadas)
        return deduped.stream()
                .filter(r -> focusedSet.contains(r.getAttributeDefinition().getId())
                        && !virtualDefIds.contains(r.getAttributeDefinition().getId()))
                .toList();
    }

    private Map<Long, Map<String, ProductAttribute>> indexAttributes(List<ProductAttribute> attributes) {
        return attributes.stream()
                .collect(Collectors.groupingBy(
                        pa -> pa.getAttributeDefinition().getId(),
                        Collectors.toMap(
                                pa -> pa.getProduct().getId(),
                                pa -> pa,
                                (a, b) -> a
                        )
                ));
    }

    // ── Construcción de la Comparación ─────────────────────────────────────────────
    private List<MapperInputs.ComparisonGroupInput> buildGroups(
            List<CategoryAttributeRule> rules,
            List<Product> products,
            Map<Long, Map<String, ProductAttribute>> attrByDefByProduct,
            Set<Long> focusedSet) {

        Map<Long, List<CategoryAttributeRule>> byGroup = rules.stream()
                .filter(CategoryAttributeRule::getIsComparable)
                .collect(Collectors.groupingBy(r -> r.getAttributeGroup().getId()));

        return byGroup.entrySet().stream()
                .map(entry -> {
                    List<CategoryAttributeRule> groupRules = entry.getValue();
                    CategoryAttributeRule first = groupRules.getFirst();

                    int groupOrder = groupRules.stream()
                            .mapToInt(CategoryAttributeRule::getDisplayOrder)
                            .min().orElse(0);

                    List<MapperInputs.ComparisonAttributeInput> attrs = groupRules.stream()
                            .sorted(Comparator.comparingInt(CategoryAttributeRule::getDisplayOrder))
                            .map(rule -> buildAttributeInput(rule, products, attrByDefByProduct, focusedSet))
                            .toList();

                    return new MapperInputs.ComparisonGroupInput(
                            first.getAttributeGroup().getName(),
                            groupOrder,
                            attrs
                    );
                })
                .sorted(Comparator.comparingInt(MapperInputs.ComparisonGroupInput::groupOrder))
                .toList();
    }

    private MapperInputs.ComparisonAttributeInput buildAttributeInput(
            CategoryAttributeRule rule,
            List<Product> products,
            Map<Long, Map<String, ProductAttribute>> attrByDefByProduct,
            Set<Long> focusedSet) {

        Long defId = rule.getAttributeDefinition().getId();

        Map<String, ProductAttribute> productValues = attrByDefByProduct.getOrDefault(defId, Map.of());

        List<MapperInputs.AttributeValueInput> values = products.stream()
                .map(p -> {
                    ProductAttribute pa = productValues.get(p.getId());
                    return new MapperInputs.AttributeValueInput(
                            p.getId(),
                            pa != null ? pa.getDisplayValue() : null,
                            pa != null ? pa.getNormalizedValue() : null
                    );
                })
                .toList();

        MapperInputs.HighlightInput highlight = computeHighlight(
                rule.getAttributeDefinition().getComparisonStrategy(), rule.getAttributeDefinition().getDataType(), values, products);

        return new MapperInputs.ComparisonAttributeInput(
                defId,
                rule.getAttributeDefinition().getDisplayName(),
                values,
                highlight
        );
    }

    private MapperInputs.HighlightInput computeHighlight(
            ComparisonStrategy strategy,
            AttributeDataType dataType,
            List<MapperInputs.AttributeValueInput> values,
            List<Product> products) {

        if (strategy == ComparisonStrategy.NEUTRAL) {
            return null;
        }

        // Para tipo de dato booleano, consideramos que indefinido = false
        List<MapperInputs.AttributeValueInput> withValue = values.stream()
                .map(v -> {
                    if (dataType == AttributeDataType.BOOLEAN && v.normalizedValue() == null) {
                        return new MapperInputs.AttributeValueInput(
                                v.productId(),
                                v.displayValue() != null ? v.displayValue() : "No",
                                BigDecimal.ZERO
                        );
                    }
                    return v;
                })
                .filter(v -> v.normalizedValue() != null)
                .toList();

        if (withValue.isEmpty()) {
            return null;
        }

        boolean allEqual = withValue.stream()
                .map(MapperInputs.AttributeValueInput::normalizedValue)
                .distinct().count() == 1;
        if (allEqual) {
            return null;
        }

        Comparator<BigDecimal> comparator = strategy == ComparisonStrategy.HIGHER_IS_BETTER
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();

        BigDecimal bestValue = withValue.stream()
                .map(MapperInputs.AttributeValueInput::normalizedValue)
                .max(comparator)
                .orElse(null);

        if (bestValue == null) {
            return null;
        }

        List<MapperInputs.AttributeValueInput> winners = withValue.stream()
                .filter(v -> v.normalizedValue() != null && v.normalizedValue().compareTo(bestValue) == 0)
                .toList();

        if (winners.isEmpty()) {
            return null;
        }

        String reason = strategy == ComparisonStrategy.HIGHER_IS_BETTER
                ? "Valor más alto"
                : "Valor más bajo";

        return new MapperInputs.HighlightInput(
                winners.stream().map(MapperInputs.AttributeValueInput::productId).toList(),
                winners.getFirst().displayValue(),
                reason
        );
    }

    // ── Atributos Faltantes ──────────────────────────────────────────────
    private List<MapperInputs.MissingAttributeInput> buildMissing(
            List<CategoryAttributeRule> rules,
            List<Product> products,
            Map<Long, Map<String, ProductAttribute>> attrByDefByProduct) {

        List<CategoryAttributeRule> required = rules.stream()
                .filter(CategoryAttributeRule::getIsRequired)
                .toList();

        List<MapperInputs.MissingAttributeInput> missing = new ArrayList<>();
        for (CategoryAttributeRule rule : required) {
            Long defId = rule.getAttributeDefinition().getId();
            Map<String, ProductAttribute> byProduct = attrByDefByProduct.getOrDefault(defId, Map.of());

            for (Product product : products) {
                if (!byProduct.containsKey(product.getId())) {
                    missing.add(new MapperInputs.MissingAttributeInput(
                            product.getId(),
                            product.getName(),
                            rule.getAttributeDefinition().getDisplayName()
                    ));
                }
            }
        }
        return missing;
    }

    // ── Atributos Virtuales ───────────────────────────────────────────────
    /**
     * Devuelve los AttributeDefinitions asociados a ProductField.
     */
    private List<AttributeDefinition> fetchVirtualDefs(Set<Long> focusedSet) {
        List<AttributeDefinition> all = attributeDefinitionRepository.findByProductFieldIsNotNull();
        if (focusedSet.isEmpty()) {
            return all;
        }
        return all.stream().filter(d -> focusedSet.contains(d.getId())).toList();
    }

    private List<MapperInputs.ComparisonAttributeInput> buildVirtualAttributeInputs(
            List<AttributeDefinition> virtualDefs,
            List<Product> products,
            Set<Long> focusedSet) {
        return virtualDefs.stream()
                .map(def -> buildVirtualAttributeInput(def, products, focusedSet))
                .toList();
    }

    private MapperInputs.ComparisonAttributeInput buildVirtualAttributeInput(
            AttributeDefinition def,
            List<Product> products,
            Set<Long> focusedSet) {

        List<MapperInputs.AttributeValueInput> values = products.stream()
                .map(p -> productFieldResolver.resolve(def.getProductField(), p))
                .toList();

        MapperInputs.HighlightInput highlight = computeHighlight(
                def.getComparisonStrategy(), def.getDataType(), values, products);

        return new MapperInputs.ComparisonAttributeInput(
                def.getId(),
                def.getDisplayName(),
                values,
                highlight
        );
    }

    // ── Diff Helper ─────────────────────────────────────────────────────
    private boolean hasDifferences(MapperInputs.ComparisonAttributeInput attr) {
        List<String> distinctValues = attr.values().stream()
                .map(MapperInputs.AttributeValueInput::displayValue)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return distinctValues.size() > 1;
    }
}
