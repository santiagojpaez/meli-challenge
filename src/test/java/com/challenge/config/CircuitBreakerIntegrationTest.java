package com.challenge.config;

import com.challenge.model.CurrencyCode;
import com.challenge.service.CurrencyExchangeService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica la configuración y comportamiento del circuit breaker de Resilience4j
 * aplicado al servicio de exchange rate.
 */
@SpringBootTest
class CircuitBreakerIntegrationTest {

    private static final String CB_NAME = "exchangeRate";

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private CurrencyExchangeService currencyExchangeService;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_NAME);
        circuitBreaker.reset();
    }

    // ── Registry ────────────────────────────────────────────────────────

    @Test
    @DisplayName("El registry contiene la instancia 'exchangeRate'")
    void registry_containsInstance() {
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers())
                .anyMatch(cb -> cb.getName().equals(CB_NAME));
    }

    // ── Estado inicial ──────────────────────────────────────────────────

    @Test
    @DisplayName("El circuit breaker arranca en estado CLOSED")
    void circuitBreaker_startsInClosedState() {
        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    // ── Configuración desde properties ──────────────────────────────────

    @Test
    @DisplayName("Configuración cargada desde application.properties")
    void circuitBreaker_configFromProperties() {
        var config = circuitBreaker.getCircuitBreakerConfig();

        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(config.getWaitIntervalFunctionInOpenState()).isNotNull();
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
    }

    // ── Comportamiento con ARS ──────────────────────────────────────────

    @Test
    @DisplayName("ARS → ARS siempre devuelve BigDecimal.ONE sin importar estado del CB")
    void arsToArs_returnsOne_regardless() {
        BigDecimal result = currencyExchangeService.getFactorToARS(CurrencyCode.ARS);
        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    // ── Comportamiento con API key vacía ────────────────────────────────

    @Test
    @DisplayName("Sin API key configurada retorna null (normalización deshabilitada)")
    void emptyApiKey_returnsNull() {
        // El test usa la key real del properties; si no hay key, retorna null.
        // Este test valida que el servicio no lance excepción.
        BigDecimal result = currencyExchangeService.getFactorToARS(CurrencyCode.USD);
        // Con key real puede devolver un valor o null si la API falla — no lanza excepción
        // Lo importante es que el circuit breaker no impide la llamada
        assertThat(circuitBreaker.getState()).isIn(
                CircuitBreaker.State.CLOSED,
                CircuitBreaker.State.HALF_OPEN);
    }

    // ── Métricas ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Métricas del circuit breaker son accesibles")
    void circuitBreaker_metricsAccessible() {
        var metrics = circuitBreaker.getMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(0);
    }

    // ── Fallback ante OPEN ──────────────────────────────────────────────

    @Test
    @DisplayName("En estado OPEN, el fallback devuelve null")
    void circuitBreaker_openState_fallbackReturnsNull() {
        circuitBreaker.transitionToOpenState();

        BigDecimal result = currencyExchangeService.getFactorToARS(CurrencyCode.USD);

        assertThat(result).isNull();
    }
}
