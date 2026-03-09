package com.challenge.dto;

import java.math.BigDecimal;

import com.challenge.model.ItemCondition;

import org.springframework.lang.Nullable;

public record ProductSummaryDTO(
        String id,
        String name,
        @Nullable String description,
        ItemCondition condition,
        @Nullable String imageUrl,
        @Nullable String color,
        @Nullable Double rating,
        PriceSummaryDTO price,
        ShippingSummaryDTO shipping
        ) {

    public record ShippingSummaryDTO(
            boolean freeShipping,
            boolean storePickup
        ) {}

    public record PriceSummaryDTO(
            BigDecimal amount,
            @Nullable BigDecimal originalAmount,
            String currency
        ) {}
}
