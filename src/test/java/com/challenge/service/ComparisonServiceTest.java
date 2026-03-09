package com.challenge.service;

import com.challenge.dto.ComparisonDTO;
import com.challenge.dto.ComparisonDiffDTO;
import com.challenge.dto.ComparisonRequestDTO;
import com.challenge.exception.CategoryMismatchException;
import com.challenge.exception.InvalidComparisonRequestException;
import com.challenge.exception.ItemNotFoundException;
import com.challenge.model.AttributeDataType;
import com.challenge.model.AttributeDefinition;
import com.challenge.model.AttributeGroup;
import com.challenge.model.Category;
import com.challenge.model.CategoryAttributeRule;
import com.challenge.model.ComparableCategory;
import com.challenge.model.ComparisonStrategy;
import com.challenge.model.ItemCondition;
import com.challenge.model.Price;
import com.challenge.model.Product;
import com.challenge.model.ProductAttribute;
import com.challenge.model.ProductField;
import com.challenge.repository.AttributeDefinitionRepository;
import com.challenge.repository.CategoryAttributeRuleRepository;
import com.challenge.repository.ComparableCategoryRepository;
import com.challenge.repository.ProductAttributeRepository;
import com.challenge.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductAttributeRepository productAttributeRepository;
    @Mock private CategoryAttributeRuleRepository ruleRepository;
    @Mock private ComparableCategoryRepository comparableCategoryRepository;
    @Mock private AttributeDefinitionRepository attributeDefinitionRepository;
    @Mock private ProductFieldResolver productFieldResolver;

    @InjectMocks
    private ComparisonService comparisonService;

    // ── Shared test fixtures ─────────────────────────────────────────────────

    private Category category;
    private Product p1;
    private Product p2;
    private AttributeGroup group;
    private AttributeDefinition defRam;
    private CategoryAttributeRule ruleRam;

    @BeforeEach
    void setupFixtures() {
        category = Category.builder().id(2L).name("Smartphones").build();

        p1 = Product.builder()
                .id("P1").name("iPhone").condition(ItemCondition.NEW)
                .category(category).build();
        p2 = Product.builder()
                .id("P2").name("Samsung").condition(ItemCondition.NEW)
                .category(category).build();

        group = AttributeGroup.builder().id(1L).name("Características").build();

        defRam = AttributeDefinition.builder()
                .id(1L).canonicalName("ram").displayName("RAM")
                .dataType(AttributeDataType.NUMBER)
                .comparisonStrategy(ComparisonStrategy.HIGHER_IS_BETTER)
                .build();

        ruleRam = CategoryAttributeRule.builder()
                .id(1L).category(category).attributeDefinition(defRam)
                .attributeGroup(group)
                .isRequired(true).isComparable(true).displayOrder(0)
                .build();
    }

    // ── compare() ────────────────────────────────────────────────────────────

    @Test
    void compare_returning200_withSameCategoryProducts() {
        ProductAttribute pa1 = pa("P1", defRam, "8 GB", BigDecimal.valueOf(8));
        ProductAttribute pa2 = pa("P2", defRam, "4 GB", BigDecimal.valueOf(4));

        mockSameCategoryCompare(List.of(pa1, pa2));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        assertThat(result.products()).hasSize(2);
        assertThat(result.attributeGroups()).hasSize(1);
        assertThat(result.missingAttributes()).isEmpty();
    }

    @Test
    void compare_throwsInvalidComparisonRequest_whenDuplicateProductIds() {
        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P1"), null);

        assertThatThrownBy(() -> comparisonService.compare(request))
                .isInstanceOf(InvalidComparisonRequestException.class);
    }

    @Test
    void compare_throwsItemNotFoundException_whenProductNotFound() {
        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "MISSING"), null);

        assertThatThrownBy(() -> comparisonService.compare(request))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void compare_throwsCategoryMismatch_whenCategoriesAreNotComparable() {
        Category otherCat = Category.builder().id(3L).name("Cafeteras").build();
        Product p3 = Product.builder()
                .id("P3").name("Cafetera").condition(ItemCondition.NEW)
                .category(otherCat).build();

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p3));
        when(comparableCategoryRepository.findPairByCategoryIds(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P3"), null);

        assertThatThrownBy(() -> comparisonService.compare(request))
                .isInstanceOf(CategoryMismatchException.class);
    }

    @Test
    void compare_throwsItemNotFoundException_whenNoAttributesFound() {
        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of());
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of(ruleRam));
        when(productAttributeRepository.findByProductIdIn(anyList())).thenReturn(List.of());

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);

        assertThatThrownBy(() -> comparisonService.compare(request))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void compare_highlightsHigherValue_forHigherIsBetterStrategy() {
        ProductAttribute pa1 = pa("P1", defRam, "12 GB", BigDecimal.valueOf(12));
        ProductAttribute pa2 = pa("P2", defRam, "8 GB", BigDecimal.valueOf(8));

        mockSameCategoryCompare(List.of(pa1, pa2));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        var attrs = result.attributeGroups().get(0).attributes();
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get(0).highlight()).isNotNull();
        assertThat(attrs.get(0).highlight().winnerIds()).containsExactly("P1");
    }

    @Test
    void compare_noHighlight_forNeutralStrategy() {
        AttributeDefinition defOs = AttributeDefinition.builder()
                .id(2L).canonicalName("os").displayName("OS")
                .dataType(AttributeDataType.TEXT)
                .comparisonStrategy(ComparisonStrategy.NEUTRAL)
                .build();
        CategoryAttributeRule ruleOs = CategoryAttributeRule.builder()
                .id(2L).category(category).attributeDefinition(defOs)
                .attributeGroup(group).isRequired(false).isComparable(true).displayOrder(1)
                .build();

        ProductAttribute pa1Os = pa("P1", defOs, "iOS", null);
        ProductAttribute pa2Os = pa("P2", defOs, "Android", null);

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of());
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of(ruleOs));
        when(productAttributeRepository.findByProductIdIn(anyList())).thenReturn(List.of(pa1Os, pa2Os));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        var attrs = result.attributeGroups().get(0).attributes();
        assertThat(attrs.get(0).highlight()).isNull();
    }

    @Test
    void compare_noHighlight_whenAllValuesAreEqual() {
        ProductAttribute pa1 = pa("P1", defRam, "8 GB", BigDecimal.valueOf(8));
        ProductAttribute pa2 = pa("P2", defRam, "8 GB", BigDecimal.valueOf(8));

        mockSameCategoryCompare(List.of(pa1, pa2));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        var attrs = result.attributeGroups().get(0).attributes();
        assertThat(attrs.get(0).highlight()).isNull();
    }

    @Test
    void compare_noHighlight_whenAllNormalizedValuesAreNull() {
        AttributeDefinition defText = AttributeDefinition.builder()
                .id(5L).canonicalName("zoom").displayName("Zoom")
                .dataType(AttributeDataType.TEXT)
                .comparisonStrategy(ComparisonStrategy.HIGHER_IS_BETTER)
                .build();
        CategoryAttributeRule ruleText = CategoryAttributeRule.builder()
                .id(5L).category(category).attributeDefinition(defText)
                .attributeGroup(group).isComparable(true).displayOrder(2)
                .build();

        ProductAttribute pa1t = pa("P1", defText, "5x", null);
        ProductAttribute pa2t = pa("P2", defText, "3x", null);

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of());
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of(ruleText));
        when(productAttributeRepository.findByProductIdIn(anyList())).thenReturn(List.of(pa1t, pa2t));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        assertThat(result.attributeGroups().get(0).attributes().get(0).highlight()).isNull();
    }

    @Test
    void compare_highlightsLowerValue_forLowerIsBetterStrategy() {
        AttributeDefinition defPrice = AttributeDefinition.builder()
                .id(3L).canonicalName("price").displayName("Precio")
                .dataType(AttributeDataType.NUMBER)
                .comparisonStrategy(ComparisonStrategy.LOWER_IS_BETTER)
                .build();
        CategoryAttributeRule rulePr = CategoryAttributeRule.builder()
                .id(3L).category(category).attributeDefinition(defPrice)
                .attributeGroup(group).isComparable(true).displayOrder(2)
                .build();

        ProductAttribute pa1p = pa("P1", defPrice, "$1000", BigDecimal.valueOf(1000));
        ProductAttribute pa2p = pa("P2", defPrice, "$500", BigDecimal.valueOf(500));

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of());
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of(rulePr));
        when(productAttributeRepository.findByProductIdIn(anyList())).thenReturn(List.of(pa1p, pa2p));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        var attr = result.attributeGroups().get(0).attributes().get(0);
        assertThat(attr.highlight().winnerIds()).containsExactly("P2");
        assertThat(attr.highlight().reason()).contains("bajo");
    }

    @Test
    void compare_booleanAttribute_treatsNullAsFalse_andHighlightsTrue() {
        AttributeDefinition defBool = AttributeDefinition.builder()
                .id(4L).canonicalName("nfc").displayName("NFC")
                .dataType(AttributeDataType.BOOLEAN)
                .comparisonStrategy(ComparisonStrategy.HIGHER_IS_BETTER)
                .build();
        CategoryAttributeRule ruleBool = CategoryAttributeRule.builder()
                .id(4L).category(category).attributeDefinition(defBool)
                .attributeGroup(group).isComparable(true).displayOrder(3)
                .build();

        // P1 has NFC=true (normalized=1), P2 has no attribute → null treated as false
        ProductAttribute pa1b = pa("P1", defBool, "Sí", BigDecimal.ONE);

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of());
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of(ruleBool));
        when(productAttributeRepository.findByProductIdIn(anyList())).thenReturn(List.of(pa1b));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        var attr = result.attributeGroups().get(0).attributes().get(0);
        assertThat(attr.highlight()).isNotNull();
        assertThat(attr.highlight().winnerIds()).containsExactly("P1");
    }

    @Test
    void compare_reportsMissingRequiredAttributes() {
        // p1 has RAM attribute, p2 does not
        ProductAttribute pa1 = pa("P1", defRam, "8 GB", BigDecimal.valueOf(8));

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of());
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of(ruleRam));
        when(productAttributeRepository.findByProductIdIn(anyList())).thenReturn(List.of(pa1));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        assertThat(result.missingAttributes()).hasSize(1);
        assertThat(result.missingAttributes().get(0).productId()).isEqualTo("P2");
        assertThat(result.missingAttributes().get(0).attributeDisplayName()).isEqualTo("RAM");
    }

    @Test
    void compare_includesVirtualAttributes_whenProductFieldIsNotNull() {
        AttributeDefinition defVirtual = AttributeDefinition.builder()
                .id(29L).canonicalName("price").displayName("Precio")
                .dataType(AttributeDataType.NUMBER)
                .comparisonStrategy(ComparisonStrategy.LOWER_IS_BETTER)
                .productField(ProductField.PRICE)
                .build();

        Price pr1 = Price.builder().amount(BigDecimal.valueOf(1000))
                .currency(com.challenge.model.CurrencyCode.ARS).build();
        Price pr2 = Price.builder().amount(BigDecimal.valueOf(2000))
                .currency(com.challenge.model.CurrencyCode.ARS).build();
        Product prod1 = Product.builder().id("P1").name("iPhone").condition(ItemCondition.NEW)
                .category(category).price(pr1).build();
        Product prod2 = Product.builder().id("P2").name("Samsung").condition(ItemCondition.NEW)
                .category(category).price(pr2).build();

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(prod1, prod2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of(defVirtual));
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of());
        // Return a non-empty list so fetchAttributes does not throw "no attributes found"
        when(productAttributeRepository.findByProductIdIn(anyList()))
                .thenReturn(List.of(pa("P1", defRam, "8 GB", BigDecimal.valueOf(8))));

        com.challenge.mapper.MapperInputs.AttributeValueInput vi1 =
                new com.challenge.mapper.MapperInputs.AttributeValueInput("P1", "1000 ARS", BigDecimal.valueOf(1000));
        com.challenge.mapper.MapperInputs.AttributeValueInput vi2 =
                new com.challenge.mapper.MapperInputs.AttributeValueInput("P2", "2000 ARS", BigDecimal.valueOf(2000));
        when(productFieldResolver.resolve(ProductField.PRICE, prod1)).thenReturn(vi1);
        when(productFieldResolver.resolve(ProductField.PRICE, prod2)).thenReturn(vi2);

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDTO result = comparisonService.compare(request);

        assertThat(result.attributeGroups()).hasSize(1);
        assertThat(result.attributeGroups().get(0).groupName()).isEqualTo("Precio y Valoración");
    }

    @Test
    void compare_withFocusedAttributeIds_filtersToRealFocusedAttributes() {
        ProductAttribute pa1 = pa("P1", defRam, "8 GB", BigDecimal.valueOf(8));
        ProductAttribute pa2 = pa("P2", defRam, "4 GB", BigDecimal.valueOf(4));

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of());
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of(ruleRam));
        when(productAttributeRepository.findByProductIdInAndAttributeDefIdIn(anyList(), anyList()))
                .thenReturn(List.of(pa1, pa2));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), List.of(1L));
        ComparisonDTO result = comparisonService.compare(request);

        assertThat(result.products()).hasSize(2);
        assertThat(result.attributeGroups()).hasSize(1);
    }

    @Test
    void compare_withFocusedVirtualOnly_returnsEmptyRealAttributes() {
        AttributeDefinition defVirtual = AttributeDefinition.builder()
                .id(29L).canonicalName("price").displayName("Precio")
                .dataType(AttributeDataType.NUMBER)
                .comparisonStrategy(ComparisonStrategy.LOWER_IS_BETTER)
                .productField(ProductField.PRICE)
                .build();

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of(defVirtual));
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of());

        com.challenge.mapper.MapperInputs.AttributeValueInput vi1 =
                new com.challenge.mapper.MapperInputs.AttributeValueInput("P1", "1000 ARS", BigDecimal.valueOf(1000));
        com.challenge.mapper.MapperInputs.AttributeValueInput vi2 =
                new com.challenge.mapper.MapperInputs.AttributeValueInput("P2", "2000 ARS", BigDecimal.valueOf(2000));
        when(productFieldResolver.resolve(any(), any())).thenReturn(vi1).thenReturn(vi2);

        // focusedAttributeIds only contains the virtual def ID
        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), List.of(29L));
        ComparisonDTO result = comparisonService.compare(request);

        // virtual group should be present (focused virtual attribute)
        assertThat(result.attributeGroups()).hasSize(1);
        assertThat(result.attributeGroups().get(0).groupName()).isEqualTo("Precio y Valoración");
    }

    // ── diff() ────────────────────────────────────────────────────────────────

    @Test
    void diff_returnsOnlyDifferentAttributes_whenValuesVary() {
        ProductAttribute pa1 = pa("P1", defRam, "8 GB", BigDecimal.valueOf(8));
        ProductAttribute pa2 = pa("P2", defRam, "4 GB", BigDecimal.valueOf(4));

        mockSameCategoryCompare(List.of(pa1, pa2));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDiffDTO result = comparisonService.diff(request);

        assertThat(result.products()).hasSize(2);
        assertThat(result.attributeGroups()).hasSize(1);
        assertThat(result.attributeGroups().get(0).attributes()).hasSize(1);
    }

    @Test
    void diff_returnsNoGroups_whenAllAttributesAreIdentical() {
        // Both products have the same display value → hasDifferences = false → filtered out
        ProductAttribute pa1 = pa("P1", defRam, "8 GB", BigDecimal.valueOf(8));
        ProductAttribute pa2 = pa("P2", defRam, "8 GB", BigDecimal.valueOf(8));

        mockSameCategoryCompare(List.of(pa1, pa2));

        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P2"), null);
        ComparisonDiffDTO result = comparisonService.diff(request);

        assertThat(result.attributeGroups()).isEmpty();
    }

    @Test
    void diff_throwsInvalidComparisonRequest_whenDuplicateProductIds() {
        ComparisonRequestDTO request = new ComparisonRequestDTO(List.of("P1", "P1"), null);

        assertThatThrownBy(() -> comparisonService.diff(request))
                .isInstanceOf(InvalidComparisonRequestException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mockSameCategoryCompare(List<ProductAttribute> attributes) {
        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(p1, p2));
        when(attributeDefinitionRepository.findByProductFieldIsNotNull()).thenReturn(List.of());
        when(ruleRepository.findByCategoryIdIn(anyList())).thenReturn(List.of(ruleRam));
        when(productAttributeRepository.findByProductIdIn(anyList())).thenReturn(attributes);
    }

    private ProductAttribute pa(String productId, AttributeDefinition def,
                                String displayValue, BigDecimal normalizedValue) {
        Product product = productId.equals("P1") ? p1 : p2;
        return ProductAttribute.builder()
                .id((long) (productId.hashCode() + def.getId()))
                .product(product)
                .attributeDefinition(def)
                .rawValue(displayValue)
                .displayValue(displayValue)
                .normalizedValue(normalizedValue)
                .build();
    }
}
