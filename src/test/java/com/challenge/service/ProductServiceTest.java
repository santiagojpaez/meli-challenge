package com.challenge.service;

import com.challenge.dto.ProductSummaryDTO;
import com.challenge.exception.ItemNotFoundException;
import com.challenge.model.Category;
import com.challenge.model.CurrencyCode;
import com.challenge.model.ItemCondition;
import com.challenge.model.Price;
import com.challenge.model.Product;
import com.challenge.model.Shipping;
import com.challenge.repository.AttributeDefinitionRepository;
import com.challenge.repository.CategoryAttributeRuleRepository;
import com.challenge.repository.CategoryRepository;
import com.challenge.repository.ProductAttributeRepository;
import com.challenge.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAttributeRepository productAttributeRepository;

    @Mock
    private CategoryAttributeRuleRepository ruleRepository;

    @Mock
    private AttributeDefinitionRepository attributeDefinitionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void listByCategory_returnsMappedProductSummaries() {
        Category category = Category.builder().id(2L).name("Smartphones").build();
        Price price = Price.builder()
                .amount(BigDecimal.valueOf(2199999))
                .currency(CurrencyCode.ARS)
                .build();
        Shipping shipping = Shipping.builder()
                .freeShipping(true)
                .storePickup(false)
                .build();
        Product product = Product.builder()
                .id("MLA2001234567")
                .name("Apple iPhone 15 Pro Max")
                .condition(ItemCondition.NEW)
                .category(category)
                .price(price)
                .shipping(shipping)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByCategory_Id(2L, pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        Page<ProductSummaryDTO> result = productService.listByCategory(2L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("MLA2001234567");
        assertThat(result.getContent().get(0).name()).isEqualTo("Apple iPhone 15 Pro Max");
        assertThat(result.getContent().get(0).condition()).isEqualTo(ItemCondition.NEW);
    }

    @Test
    void search_usesDescendantIdsAndDelegatesToRepository() {
        List<Long> descendants = List.of(2L, 5L, 7L);
        Pageable pageable = PageRequest.of(0, 10);

        when(categoryRepository.findDescendantIds(1L)).thenReturn(descendants);
        when(productRepository.searchByText(descendants, "iphone", pageable))
                .thenReturn(Page.empty(pageable));

        Page<ProductSummaryDTO> result = productService.search(1L, "iphone", pageable);

        assertThat(result.getContent()).isEmpty();
        verify(categoryRepository).findDescendantIds(1L);
        verify(productRepository).searchByText(descendants, "iphone", pageable);
    }

    @Test
    void getDetail_throwsItemNotFoundException_whenProductDoesNotExist() {
        when(productRepository.findWithRelationsById("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getDetail("NONEXISTENT"))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }
}
