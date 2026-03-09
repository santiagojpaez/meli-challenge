package com.challenge.service;

import com.challenge.model.CurrencyCode;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Obtiene y cachea ratios de conversión de monedas a ARS usando ExchangeRate API v6.
 *
 * Endpoint usado: GET /v6/{apiKey}/pair/{CURRENCY}/ARS
 * Campo de respuesta: conversion_rate (BigDecimal)
 *
 * Los ratios son cacheados por Spring Cache (Caffeine, TTL 12h).
 * Un circuit breaker (Resilience4j) evita llamadas repetidas a una API caída.
 * Ante fallos devuelve {@code null} para tratar normalizedValue como no disponible.
 */
@Service
@Slf4j
public class CurrencyExchangeService {

    private static final CurrencyCode BASE = CurrencyCode.ARS;

    private final String apiKey;
    private final String apiBaseUrl;
    private final RestClient restClient;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @org.springframework.beans.factory.annotation.Autowired
    public CurrencyExchangeService(
            @Value("${exchange.rate.api.key:}") String apiKey,
            @Value("${exchange.rate.api.base-url:https://v6.exchangerate-api.com/v6}") String apiBaseUrl) {
        this.apiKey = apiKey;
        this.apiBaseUrl = apiBaseUrl;
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                                .withConnectTimeout(CONNECT_TIMEOUT)
                                .withReadTimeout(READ_TIMEOUT)))
                .build();
    }

    /** Package-private constructor para testeo unitario con RestClient incorporado. */
    CurrencyExchangeService(String apiKey, String apiBaseUrl, RestClient restClient) {
        this.apiKey = apiKey;
        this.apiBaseUrl = apiBaseUrl;
        this.restClient = restClient;
    }

    @Cacheable(value = "exchangeRates", key = "#currency.name()")
    @CircuitBreaker(name = "exchangeRate", fallbackMethod = "fallbackRate")
    public BigDecimal getFactorToARS(CurrencyCode currency) {
        if (currency == BASE) {
            return BigDecimal.ONE;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Exchange rate API key not configured — price normalization disabled");
            return null;
        }

        return fetchRate(currency);
    }

    @SuppressWarnings("unused")
    private BigDecimal fallbackRate(CurrencyCode currency, Throwable t) {
        log.warn("Circuit breaker fallback for {} → ARS: {}", currency, t.getMessage());
        return null;
    }

    private BigDecimal fetchRate(CurrencyCode currency) {
        String url = apiBaseUrl + "/" + apiKey + "/pair/" + currency.name() + "/" + BASE.name();
        ExchangeRateApiResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(ExchangeRateApiResponse.class);

        if (response == null || !"success".equals(response.result()) || response.conversionRate() == null) {
            log.warn("Unexpected exchange rate response for {} → ARS: {}", currency, response);
            return null;
        }

        log.debug("Fetched exchange rate {} → ARS = {}", currency, response.conversionRate());
        return response.conversionRate();
    }
}
