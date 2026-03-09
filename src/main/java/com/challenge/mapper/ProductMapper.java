package com.challenge.mapper;

import com.challenge.dto.AttributeGroupValueDTO;
import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.ProductDetailDTO;
import com.challenge.dto.ProductSummaryDTO;
import com.challenge.model.Price;
import com.challenge.model.Product;
import com.challenge.model.Shipping;

import java.util.List;
import java.util.stream.Collectors;

public final class ProductMapper {

    private ProductMapper() {
    }


    public static ProductSummaryDTO toSummaryDTO(Product product) {
        if (product == null) {
            return null;
        }
        ProductSummaryDTO.PriceSummaryDTO priceDto = product.getPrice() != null
                ? toPriceDTO(product.getPrice())
                : null;
        ProductSummaryDTO.ShippingSummaryDTO shippingDto = product.getShipping() != null
                ? toShippingDTO(product.getShipping())
                : null;
        return new ProductSummaryDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCondition(),
                product.getImageUrl(),
                product.getColor(),
                product.getRating(),
                priceDto,
                shippingDto
        );
    }


    public static ProductDetailDTO toDetailDTO(Product product, List<MapperInputs.AttributeGroupValueInput> attributeGroups) {
        if (product == null) {
            return null;
        }
        ProductSummaryDTO productSummary = toSummaryDTO(product);
        CategorySummaryDTO categoryDto = product.getCategory() != null
                ? CategoryMapper.toSummaryDTO(product.getCategory())
                : null;
        List<AttributeGroupValueDTO> groups = attributeGroups == null
                ? List.of()
                : attributeGroups.stream()
                        .map(ProductMapper::toAttributeGroupValueDTO)
                        .collect(Collectors.toList());
        return new ProductDetailDTO(
                productSummary,
                product.getWeight(),
                product.getSize(),
                product.getAvailableQuantity(),
                product.getSoldQuantity(),
                categoryDto,
                groups
        );
    }

    public static AttributeGroupValueDTO toAttributeGroupValueDTO(MapperInputs.AttributeGroupValueInput input) {
        if (input == null) {
            return null;
        }
        List<AttributeGroupValueDTO.AttributeValueDisplayDTO> attrs = input.attributes() == null
                ? List.of()
                : input.attributes().stream()
                        .map(ProductMapper::toAttributeValueDisplayDTO)
                        .collect(Collectors.toList());
        return new AttributeGroupValueDTO(input.groupId(), input.groupName(), attrs);
    }

    public static AttributeGroupValueDTO.AttributeValueDisplayDTO toAttributeValueDisplayDTO(
            MapperInputs.AttributeGroupValueInput.AttributeValueDisplayInput input) {
        if (input == null) {
            return null;
        }
        return new AttributeGroupValueDTO.AttributeValueDisplayDTO(input.displayName(), input.displayValue());
    }


    public static ProductSummaryDTO.PriceSummaryDTO toPriceDTO(Price price) {
        if (price == null) {
            return null;
        }
        String currency = price.getCurrency() != null
                ? price.getCurrency().name()
                : null;
        return new ProductSummaryDTO.PriceSummaryDTO(
                price.getAmount(),
                price.getOriginalAmount(),
                currency
        );
    }

    public static ProductSummaryDTO.ShippingSummaryDTO toShippingDTO(Shipping shipping) {
        if (shipping == null) {
            return null;
        }
        return new ProductSummaryDTO.ShippingSummaryDTO(
                Boolean.TRUE.equals(shipping.getFreeShipping()),
                Boolean.TRUE.equals(shipping.getStorePickup())
        );
    }
}
