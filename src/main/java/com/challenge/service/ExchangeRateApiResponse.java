package com.challenge.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Response tipada de exchangerate-api.com v6 pair endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ExchangeRateApiResponse(
        String result,

        @JsonProperty("base_code")
        String baseCode,

        @JsonProperty("target_code")
        String targetCode,

        @JsonProperty("conversion_rate")
        BigDecimal conversionRate
) {}
