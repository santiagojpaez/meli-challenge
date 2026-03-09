package com.challenge.repository;

import com.challenge.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class RepositoryTest {

    @Autowired
    TestEntityManager entityManager;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    AttributeDefinitionRepository attributeDefinitionRepository;
    @Autowired
    CategoryAttributeRuleRepository categoryAttributeRuleRepository;
    @Autowired
    ComparableCategoryRepository comparableCategoryRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ProductAttributeRepository productAttributeRepository;

    @Test
    void shouldFindCategoriesWithChildren() {
        Category parent = categoryRepository.save(Category.builder().name("Tech").build());
        categoryRepository.save(Category.builder().name("Phones").parent(parent).build());
        categoryRepository.save(Category.builder().name("Laptops").parent(parent).build());
        entityManager.flush();
        entityManager.clear();

        List<Category> categories = categoryRepository.findAllWithChildren();

        assertThat(categories).isNotEmpty();
        Category tech = categories.stream().filter(c -> "Tech".equals(c.getName())).findFirst().orElseThrow();
        assertThat(tech.getChildren()).hasSize(2);
        assertThat(tech.getChildren().stream().map(Category::getName)).containsExactlyInAnyOrder("Phones", "Laptops");
    }

    @Test
    void shouldFindAttributesDefinitionsForSiblingCategories() {
        Category cat1 = categoryRepository.save(Category.builder().name("Cat1").build());
        Category cat2 = categoryRepository.save(Category.builder().name("Cat2").build());

        AttributeDefinition def = attributeDefinitionRepository.save(
                AttributeDefinition.builder()
                        .canonicalName("ram_test")
                        .displayName("RAM")
                        .dataType(AttributeDataType.NUMBER)
                        .comparisonStrategy(ComparisonStrategy.HIGHER_IS_BETTER)
                        .build());
        AttributeGroup group = entityManager.persist(AttributeGroup.builder()
                .name("Main").build()); 

        categoryAttributeRuleRepository.save(CategoryAttributeRule.builder()
                .category(cat1)
                .attributeDefinition(def)
                .attributeGroup(group)
                .build());
        categoryAttributeRuleRepository.save(CategoryAttributeRule.builder()
                .category(cat2)
                .attributeDefinition(def)
                .attributeGroup(group)
                .build());

        List<AttributeDefinition> definitions = attributeDefinitionRepository
                .findDistinctByCategoryAttributeRules_Category_IdIn(List.of(cat1.getId(), cat2.getId()));

        assertThat(definitions).hasSize(1);
        assertThat(definitions.get(0).getCanonicalName()).isEqualTo("ram_test");
    }

    @Test
    void shouldFindCategoryAttributeRulesWithAttributeDefinitionAndGroup() {
        Category cat = categoryRepository.save(Category.builder().name("Electronics").build());
        AttributeDefinition def = attributeDefinitionRepository.save(
                AttributeDefinition.builder()
                        .canonicalName("screen_test")
                        .displayName("Screen")
                        .dataType(AttributeDataType.TEXT)
                        .comparisonStrategy(ComparisonStrategy.NEUTRAL)
                        .build());
        AttributeGroup group = entityManager.persist(AttributeGroup.builder()
                .name("Display").build());
        categoryAttributeRuleRepository.save(CategoryAttributeRule.builder()
                .category(cat)
                .attributeDefinition(def)
                .attributeGroup(group)
                .build());

        List<CategoryAttributeRule> rules = categoryAttributeRuleRepository.findByCategoryIdIn(List.of(cat.getId()));

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getAttributeDefinition().getCanonicalName()).isEqualTo("screen_test");
        assertThat(rules.get(0).getAttributeGroup()).isNotNull();
    }

    @Test
    void shouldFindComparableCategoryPairs() {
        Category catA = categoryRepository.save(Category.builder().name("Tablets").build());
        Category catB = categoryRepository.save(Category.builder().name("Phones").build());
        comparableCategoryRepository.save(ComparableCategory.builder().categoryA(catA).categoryB(catB).build());

        List<ComparableCategory> pairs = comparableCategoryRepository
                .findAllByCategoryId(catA.getId());

        assertThat(pairs).hasSize(1);
        assertThat(pairs.get(0).getCategoryA().getId()).isEqualTo(catA.getId());
        assertThat(pairs.get(0).getCategoryB().getId()).isEqualTo(catB.getId());
    }

    @Test
    void shouldSearchProductsByNameAndBrandAndModel() {
        Category cat = categoryRepository.save(Category.builder().name("Smartphones").build());
        Product product = productRepository.save(Product.builder()
                .id("MLA001")
                .name("iPhone 15 Apple")
                .condition(ItemCondition.NEW)
                .category(cat)
                .price(Price.builder().amount(new BigDecimal("999")).currency(CurrencyCode.ARS).build())
                .shipping(Shipping.builder().freeShipping(true).build())
                .build());
        AttributeDefinition brandDef = attributeDefinitionRepository.save(
                AttributeDefinition.builder()
                        .canonicalName("brand")
                        .displayName("Brand")
                        .dataType(AttributeDataType.TEXT)
                        .comparisonStrategy(ComparisonStrategy.NEUTRAL)
                        .build());
        entityManager.persist(ProductAttribute.builder()
                .product(product)
                .attributeDefinition(brandDef)
                .rawValue("Apple")
                .build());
        entityManager.flush();

        var pageResult = productRepository.searchByText(List.of(cat.getId()), "Apple", PageRequest.of(0, 10));

        assertThat(pageResult.getContent()).hasSize(1);
        assertThat(pageResult.getContent().get(0).getName()).contains("iPhone");
    }

    @Test
    void shouldFindProductAttributesWithDefinition() {
        Category cat = categoryRepository.save(Category.builder().name("Gadgets").build());
        Product p1 = productRepository.save(Product.builder()
                .id("MLA101")
                .name("Product A")
                .condition(ItemCondition.NEW)
                .category(cat)
                .price(Price.builder().amount(new BigDecimal("500")).currency(CurrencyCode.USD).build())
                .shipping(Shipping.builder().freeShipping(false).build())
                .build());
        Product p2 = productRepository.save(Product.builder()
                .id("MLA102")
                .name("Product B")
                .condition(ItemCondition.NEW)
                .category(cat)
                .price(Price.builder().amount(new BigDecimal("600")).currency(CurrencyCode.USD).build())
                .shipping(Shipping.builder().freeShipping(false).build())
                .build());
        AttributeDefinition def = attributeDefinitionRepository.save(
                AttributeDefinition.builder()
                        .canonicalName("color_attr")
                        .displayName("Color")
                        .dataType(AttributeDataType.TEXT)
                        .comparisonStrategy(ComparisonStrategy.NEUTRAL)
                        .build());
        entityManager.persist(ProductAttribute.builder()
                .product(p1).attributeDefinition(def).rawValue("Black").build());
        entityManager.persist(ProductAttribute.builder()
                .product(p2).attributeDefinition(def).rawValue("White").build());
        entityManager.flush();

        List<ProductAttribute> attributes = productAttributeRepository.findByProductIdIn(List.of("MLA101", "MLA102"));

        assertThat(attributes).hasSize(2);
        attributes.forEach(pa -> {
            assertThat(pa.getAttributeDefinition()).isNotNull();
            assertThat(pa.getAttributeDefinition().getCanonicalName()).isEqualTo("color_attr");
        });
    }
}
