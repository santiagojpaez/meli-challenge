package com.challenge.mapper;

import com.challenge.dto.AttributeGroupValueDTO;
import com.challenge.dto.ProductDetailDTO;
import com.challenge.dto.ProductSummaryDTO;
import com.challenge.model.Category;
import com.challenge.model.CurrencyCode;
import com.challenge.model.ItemCondition;
import com.challenge.model.Price;
import com.challenge.model.Product;
import com.challenge.model.Shipping;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    // ── toSummaryDTO ──────────────────────────────────────────────────────────

    @Test
    void toSummaryDTO_returnsNull_whenProductIsNull() {
        assertThat(ProductMapper.toSummaryDTO(null)).isNull();
    }

    @Test
    void toSummaryDTO_mapsAllFields_withPriceAndShipping() {
        Price price = Price.builder()
                .amount(BigDecimal.valueOf(1000))
                .originalAmount(BigDecimal.valueOf(1200))
                .currency(CurrencyCode.ARS)
                .build();
        Shipping shipping = Shipping.builder().freeShipping(true).storePickup(false).build();
        Product product = Product.builder()
                .id("P1").name("iPhone").description("Desc")
                .condition(ItemCondition.NEW)
                .imageUrl("https://img.com/p1.jpg")
                .color("Red").rating(4.8)
                .price(price).shipping(shipping)
                .build();

        ProductSummaryDTO dto = ProductMapper.toSummaryDTO(product);

        assertThat(dto.id()).isEqualTo("P1");
        assertThat(dto.name()).isEqualTo("iPhone");
        assertThat(dto.description()).isEqualTo("Desc");
        assertThat(dto.condition()).isEqualTo(ItemCondition.NEW);
        assertThat(dto.color()).isEqualTo("Red");
        assertThat(dto.rating()).isEqualTo(4.8);
        assertThat(dto.price().amount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(dto.price().originalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        assertThat(dto.price().currency()).isEqualTo("ARS");
        assertThat(dto.shipping().freeShipping()).isTrue();
        assertThat(dto.shipping().storePickup()).isFalse();
    }

    @Test
    void toSummaryDTO_handlesNullPriceAndShipping() {
        Product product = Product.builder()
                .id("P1").name("Test").condition(ItemCondition.USED)
                .build();

        ProductSummaryDTO dto = ProductMapper.toSummaryDTO(product);

        assertThat(dto.price()).isNull();
        assertThat(dto.shipping()).isNull();
    }

    // ── toPriceDTO ────────────────────────────────────────────────────────────

    @Test
    void toPriceDTO_returnsNull_whenPriceIsNull() {
        assertThat(ProductMapper.toPriceDTO(null)).isNull();
    }

    @Test
    void toPriceDTO_handlesNullCurrency() {
        Price price = Price.builder().amount(BigDecimal.valueOf(500)).currency(null).build();

        ProductSummaryDTO.PriceSummaryDTO dto = ProductMapper.toPriceDTO(price);

        assertThat(dto.currency()).isNull();
        assertThat(dto.amount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    // ── toShippingDTO ─────────────────────────────────────────────────────────

    @Test
    void toShippingDTO_returnsNull_whenShippingIsNull() {
        assertThat(ProductMapper.toShippingDTO(null)).isNull();
    }

    @Test
    void toShippingDTO_handlesNullBooleans() {
        Shipping shipping = Shipping.builder().freeShipping(null).storePickup(null).build();

        ProductSummaryDTO.ShippingSummaryDTO dto = ProductMapper.toShippingDTO(shipping);

        assertThat(dto.freeShipping()).isFalse();
        assertThat(dto.storePickup()).isFalse();
    }

    // ── toDetailDTO ───────────────────────────────────────────────────────────

    @Test
    void toDetailDTO_returnsNull_whenProductIsNull() {
        assertThat(ProductMapper.toDetailDTO(null, List.of())).isNull();
    }

    @Test
    void toDetailDTO_mapsWeightSizeQuantityAndCategory() {
        Category cat = Category.builder().id(2L).name("Smartphones").build();
        Product product = Product.builder()
                .id("P1").name("Test").condition(ItemCondition.NEW)
                .weight(BigDecimal.valueOf(200))
                .size("150x70x8 mm")
                .availableQuantity(10).soldQuantity(50)
                .category(cat)
                .build();

        MapperInputs.AttributeGroupValueInput groupInput = new MapperInputs.AttributeGroupValueInput(
                1L, "Características",
                List.of(new MapperInputs.AttributeGroupValueInput.AttributeValueDisplayInput("RAM", "8 GB"))
        );

        ProductDetailDTO dto = ProductMapper.toDetailDTO(product, List.of(groupInput));

        assertThat(dto.productSummary().id()).isEqualTo("P1");
        assertThat(dto.weight()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(dto.size()).isEqualTo("150x70x8 mm");
        assertThat(dto.availableQuantity()).isEqualTo(10);
        assertThat(dto.soldQuantity()).isEqualTo(50);
        assertThat(dto.category().name()).isEqualTo("Smartphones");
        assertThat(dto.attributeGroups()).hasSize(1);
        assertThat(dto.attributeGroups().get(0).groupName()).isEqualTo("Características");
    }

    @Test
    void toDetailDTO_returnsEmptyGroups_whenAttributeGroupsIsNull() {
        Product product = Product.builder()
                .id("P1").name("Test").condition(ItemCondition.NEW).build();

        ProductDetailDTO dto = ProductMapper.toDetailDTO(product, null);

        assertThat(dto.attributeGroups()).isEmpty();
    }

    // ── toAttributeGroupValueDTO ──────────────────────────────────────────────

    @Test
    void toAttributeGroupValueDTO_returnsNull_whenInputIsNull() {
        assertThat(ProductMapper.toAttributeGroupValueDTO(null)).isNull();
    }

    @Test
    void toAttributeGroupValueDTO_returnsEmptyAttributes_whenAttributeListIsNull() {
        MapperInputs.AttributeGroupValueInput input =
                new MapperInputs.AttributeGroupValueInput(1L, "Group", null);

        AttributeGroupValueDTO dto = ProductMapper.toAttributeGroupValueDTO(input);

        assertThat(dto.groupName()).isEqualTo("Group");
        assertThat(dto.attributes()).isEmpty();
    }

    // ── toAttributeValueDisplayDTO ────────────────────────────────────────────

    @Test
    void toAttributeValueDisplayDTO_returnsNull_whenInputIsNull() {
        assertThat(ProductMapper.toAttributeValueDisplayDTO(null)).isNull();
    }

    @Test
    void toAttributeValueDisplayDTO_mapsDisplayNameAndValue() {
        MapperInputs.AttributeGroupValueInput.AttributeValueDisplayInput input =
                new MapperInputs.AttributeGroupValueInput.AttributeValueDisplayInput("RAM", "8 GB");

        var dto = ProductMapper.toAttributeValueDisplayDTO(input);

        assertThat(dto.displayName()).isEqualTo("RAM");
        assertThat(dto.displayValue()).isEqualTo("8 GB");
    }
}
