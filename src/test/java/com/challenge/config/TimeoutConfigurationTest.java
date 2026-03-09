package com.challenge.config;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración que verifican que las propiedades de timeout,
 * connection pool y caché se cargan correctamente al levantar el contexto.
 *
 * No ejercita timeouts reales (eso requeriría retrasos artificiales),
 * sino que verifica que la configuración está aplicada donde corresponde.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "exchange.rate.api.key=",
        "rate-limit.default-per-minute=10000"
})
class TimeoutConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CacheManager cacheManager;

    @Value("${spring.mvc.async.request-timeout}")
    private long mvcAsyncTimeout;

    @Value("${spring.lifecycle.timeout-per-shutdown-phase}")
    private String shutdownTimeout;

    @Value("${spring.jpa.properties.jakarta.persistence.query.timeout}")
    private int queryTimeout;

    @Value("${spring.datasource.hikari.connection-timeout}")
    private long hikariConnectionTimeout;

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int hikariMaxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle}")
    private int hikariMinIdle;

    @Value("${spring.datasource.hikari.idle-timeout}")
    private long hikariIdleTimeout;

    // ── Server Timeouts ──────────────────────────────────────────────────

    @Test
    void mvcAsyncRequestTimeout_isConfigured() {
        assertThat(mvcAsyncTimeout).isEqualTo(30000);
    }

    @Test
    void gracefulShutdownTimeout_isConfigured() {
        assertThat(shutdownTimeout).isEqualTo("30s");
    }

    // ── JPA Query Timeout ────────────────────────────────────────────────

    @Test
    void jpaQueryTimeout_isConfigured() {
        assertThat(queryTimeout).isEqualTo(5000);
    }

    // ── HikariCP Connection Pool ─────────────────────────────────────────

    @Test
    void hikariConnectionPool_isCorrectlyConfigured() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);

        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertThat(hikari.getConnectionTimeout()).isEqualTo(5000);
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(10);
        assertThat(hikari.getMinimumIdle()).isEqualTo(2);
        assertThat(hikari.getIdleTimeout()).isEqualTo(300000);
    }

    @Test
    void hikariConnectionTimeout_propertyMatchesActualConfig() {
        assertThat(hikariConnectionTimeout).isEqualTo(5000);
    }

    @Test
    void hikariMaxPoolSize_propertyMatchesActualConfig() {
        assertThat(hikariMaxPoolSize).isEqualTo(10);
    }

    @Test
    void hikariMinIdle_propertyMatchesActualConfig() {
        assertThat(hikariMinIdle).isEqualTo(2);
    }

    @Test
    void hikariIdleTimeout_propertyMatchesActualConfig() {
        assertThat(hikariIdleTimeout).isEqualTo(300000);
    }

    // ── Cache Manager ────────────────────────────────────────────────────

    @Test
    void cacheManager_isCaffeineBasedAndHasAllCaches() {
        assertThat(cacheManager.getClass().getSimpleName()).contains("Caffeine");

        // Todos los caches configurados existen
        assertThat(cacheManager.getCache("categoryTree")).isNotNull();
        assertThat(cacheManager.getCache("categoryDetail")).isNotNull();
        assertThat(cacheManager.getCache("categoryAttributes")).isNotNull();
        assertThat(cacheManager.getCache("productDetail")).isNotNull();
        assertThat(cacheManager.getCache("exchangeRates")).isNotNull();
    }
}
