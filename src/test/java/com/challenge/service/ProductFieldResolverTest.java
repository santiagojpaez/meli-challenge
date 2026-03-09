package com.challenge.service;

import com.challenge.mapper.MapperInputs;
import com.challenge.model.CurrencyCode;
import com.challenge.model.ItemCondition;
import com.challenge.model.Price;
import com.challenge.model.Product;
import com.challenge.model.ProductField;
import com.challenge.model.Shipping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductFieldResolverTest {

    @Mock
    private CurrencyExchangeService currencyExchangeService;

    @InjectMocks
    private ProductFieldResolver resolver;

    // ── PRICE ────────────────────────────────────────────────────────────────

    @Test
    void resolvePrice_returnsNormalizedValue_whenExchangeRateAvailable() {
        when(currencyExchangeService.getFactorToARS(CurrencyCode.ARS)).thenReturn(BigDecimal.ONE);

        Product product = productWithPrice(BigDecimal.valueOf(1000), null, CurrencyCode.ARS);

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.PRICE, product);

        assertThat(result.productId()).isEqualTo("P1");
        assertThat(result.displayValue()).isEqualTo("1000 ARS");
        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void resolvePrice_returnsNullNormalizedValue_whenExchangeRateUnavailable() {
        when(currencyExchangeService.getFactorToARS(CurrencyCode.USD)).thenReturn(null);

        Product product = productWithPrice(BigDecimal.valueOf(100), null, CurrencyCode.USD);

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.PRICE, product);

        assertThat(result.productId()).isEqualTo("P1");
        assertThat(result.displayValue()).isEqualTo("100 USD");
        assertThat(result.normalizedValue()).isNull();
    }

    @Test
    void resolvePrice_returnsNullDisplayAndNormalized_whenProductHasNoPrice() {
        Product product = Product.builder().id("P1").name("X").condition(ItemCondition.NEW).build();

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.PRICE, product);

        assertThat(result.productId()).isEqualTo("P1");
        assertThat(result.displayValue()).isNull();
        assertThat(result.normalizedValue()).isNull();
    }

    @Test
    void resolvePrice_appliesExchangeRateFactor_whenForeignCurrency() {
        when(currencyExchangeService.getFactorToARS(CurrencyCode.USD)).thenReturn(BigDecimal.valueOf(1000));

        Product product = productWithPrice(BigDecimal.valueOf(10), null, CurrencyCode.USD);

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.PRICE, product);

        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    // ── DISCOUNT ─────────────────────────────────────────────────────────────

    @Test
    void resolveDiscount_returnsZeroPercent_whenNoOriginalAmount() {
        Product product = productWithPrice(BigDecimal.valueOf(900), null, CurrencyCode.ARS);

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.DISCOUNT, product);

        assertThat(result.displayValue()).isEqualTo("0%");
        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void resolveDiscount_returnsZeroPercent_whenOriginalAmountIsZero() {
        Product product = productWithPrice(BigDecimal.valueOf(900), BigDecimal.ZERO, CurrencyCode.ARS);

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.DISCOUNT, product);

        assertThat(result.displayValue()).isEqualTo("0%");
    }

    @Test
    void resolveDiscount_returnsCorrectPercentage_whenOriginalAmountIsHigher() {
        Product product = productWithPrice(BigDecimal.valueOf(900), BigDecimal.valueOf(1000), CurrencyCode.ARS);

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.DISCOUNT, product);

        assertThat(result.displayValue()).isEqualTo("10.0%");
        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
    }

    @Test
    void resolveDiscount_returnsZero_whenPriceIsNull() {
        Product product = Product.builder().id("P1").name("X").condition(ItemCondition.NEW).build();

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.DISCOUNT, product);

        assertThat(result.displayValue()).isEqualTo("0%");
        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── FREE_SHIPPING ─────────────────────────────────────────────────────────

    @Test
    void resolveFreeShipping_returnsSi_whenFreeShippingIsTrue() {
        Product product = productWithShipping(true);

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.FREE_SHIPPING, product);

        assertThat(result.displayValue()).isEqualTo("Sí");
        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void resolveFreeShipping_returnsNo_whenFreeShippingIsFalse() {
        Product product = productWithShipping(false);

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.FREE_SHIPPING, product);

        assertThat(result.displayValue()).isEqualTo("No");
        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void resolveFreeShipping_returnsNo_whenShippingIsNull() {
        Product product = Product.builder().id("P1").name("X").condition(ItemCondition.NEW).build();

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.FREE_SHIPPING, product);

        assertThat(result.displayValue()).isEqualTo("No");
        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── RATING ───────────────────────────────────────────────────────────────

    @Test
    void resolveRating_returnsNullValues_whenRatingIsNull() {
        Product product = Product.builder().id("P1").name("X").condition(ItemCondition.NEW).build();

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.RATING, product);

        assertThat(result.productId()).isEqualTo("P1");
        assertThat(result.displayValue()).isNull();
        assertThat(result.normalizedValue()).isNull();
    }

    @Test
    void resolveRating_returnsNormalizedRating_whenRatingIsSet() {
        Product product = Product.builder()
                .id("P1").name("X").condition(ItemCondition.NEW)
                .rating(4.5)
                .build();

        MapperInputs.AttributeValueInput result = resolver.resolve(ProductField.RATING, product);

        assertThat(result.displayValue()).isEqualTo("4.5");
        assertThat(result.normalizedValue()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product productWithPrice(BigDecimal amount, BigDecimal original, CurrencyCode currency) {
        Price price = Price.builder()
                .amount(amount)
                .originalAmount(original)
                .currency(currency)
                .build();
        return Product.builder()
                .id("P1").name("X").condition(ItemCondition.NEW)
                .price(price)
                .build();
    }

    private Product productWithShipping(boolean free) {
        Shipping shipping = Shipping.builder().freeShipping(free).storePickup(false).build();
        return Product.builder()
                .id("P1").name("X").condition(ItemCondition.NEW)
                .shipping(shipping)
                .build();
    }
}
