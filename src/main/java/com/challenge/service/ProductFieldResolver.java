package com.challenge.service;

import com.challenge.mapper.MapperInputs;
import com.challenge.model.Product;
import com.challenge.model.ProductField;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Extrae el normalizedValue y displayValue para los atributos existentes en 
 * el modelo Product. Los mismos se manejan de manera separada a los ProductAttributes
 *
 * Para agregar un nuevo atributo virtual:
 *  1. Agrega un valor al enum {@link ProductField}.
 *  2. Agrega un case en {@link #resolve(ProductField, Product)}.
 *  3. Inserta el registro correspondientes en attribute_definitions.
 */
@Component
public class ProductFieldResolver {

    private final CurrencyExchangeService currencyExchangeService;

    public ProductFieldResolver(CurrencyExchangeService currencyExchangeService) {
        this.currencyExchangeService = currencyExchangeService;
    }

    /**
     * @return an AttributeValueInput for the given product, or null if the value is unavailable.
     */
    public MapperInputs.AttributeValueInput resolve(ProductField field, Product product) {
        return switch (field) {
            case PRICE -> resolvePrice(product);
            case DISCOUNT -> resolveDiscount(product);
            case FREE_SHIPPING -> resolveFreeShipping(product);
            case RATING -> resolveRating(product);
        };
    }

    private MapperInputs.AttributeValueInput resolvePrice(Product product) {
        if (product.getPrice() == null) {
            return new MapperInputs.AttributeValueInput(product.getId(), null, null);
        }
        String display = product.getPrice().getAmount().stripTrailingZeros().toPlainString()
                + " " + product.getPrice().getCurrency().name();

        // Normalize to ARS using live exchange rate; null if rate unavailable
        BigDecimal factor = currencyExchangeService.getFactorToARS(product.getPrice().getCurrency());
        BigDecimal normalizedValue = factor != null
                ? product.getPrice().getAmount().multiply(factor).setScale(2, RoundingMode.HALF_UP)
                : null;

        return new MapperInputs.AttributeValueInput(product.getId(), display, normalizedValue);
    }

    private MapperInputs.AttributeValueInput resolveDiscount(Product product) {
        if (product.getPrice() == null) {
            return new MapperInputs.AttributeValueInput(product.getId(), "0%", BigDecimal.ZERO);
        }
        BigDecimal original = product.getPrice().getOriginalAmount();
        if (original == null || original.compareTo(BigDecimal.ZERO) == 0) {
            return new MapperInputs.AttributeValueInput(product.getId(), "0%", BigDecimal.ZERO);
        }
        BigDecimal amount = product.getPrice().getAmount();
        BigDecimal pct = original.subtract(amount)
                .multiply(BigDecimal.valueOf(100))
                .divide(original, 1, RoundingMode.HALF_UP);
        return new MapperInputs.AttributeValueInput(product.getId(), pct.toPlainString() + "%", pct);
    }

    private MapperInputs.AttributeValueInput resolveFreeShipping(Product product) {
        if (product.getShipping() == null) {
            return new MapperInputs.AttributeValueInput(product.getId(), "No", BigDecimal.ZERO);
        }
        boolean free = Boolean.TRUE.equals(product.getShipping().getFreeShipping());
        return new MapperInputs.AttributeValueInput(
                product.getId(),
                free ? "Sí" : "No",
                free ? BigDecimal.ONE : BigDecimal.ZERO
        );
    }

    private MapperInputs.AttributeValueInput resolveRating(Product product) {
        Double rating = product.getRating();
        if (rating == null) {
            return new MapperInputs.AttributeValueInput(product.getId(), null, null);
        }
        BigDecimal normalized = BigDecimal.valueOf(rating);
        return new MapperInputs.AttributeValueInput(product.getId(), rating.toString(), normalized);
    }
}
