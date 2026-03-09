package com.challenge.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

/**
 * Tests de integración para el filtro de rate limiting.
 *
 * Usa límites muy bajos (2-3 req/min) para verificar rápidamente que el filtro
 * bloquea requests excedentes con 429 y headers correctos.
 *
 * Desactiva la API key de exchange rate para evitar llamadas externas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "exchange.rate.api.key=",
        "rate-limit.comparisons-per-minute=2",
        "rate-limit.search-per-minute=3",
        "rate-limit.default-per-minute=3",
        "rate-limit.window-ms=60000"
})
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Rate Limit Headers ───────────────────────────────────────────────

    @Test
    void apiRequest_includesRateLimitHeaders() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"));
    }

    @Test
    void rateLimitRemainingHeader_decrementsOnEachRequest() throws Exception {
        // Primer request: Remaining = limit - 1
        MvcResult first = mockMvc.perform(get("/api/categories").header("X-Forwarded-For", "10.0.0.1"))
                .andExpect(status().isOk())
                .andReturn();

        int limit = Integer.parseInt(first.getResponse().getHeader("X-RateLimit-Limit"));
        int remainingFirst = Integer.parseInt(first.getResponse().getHeader("X-RateLimit-Remaining"));
        assertThat(remainingFirst).isEqualTo(limit - 1);

        // Segundo request: Remaining = limit - 2
        MvcResult second = mockMvc.perform(get("/api/categories").header("X-Forwarded-For", "10.0.0.1"))
                .andExpect(status().isOk())
                .andReturn();

        int remainingSecond = Integer.parseInt(second.getResponse().getHeader("X-RateLimit-Remaining"));
        assertThat(remainingSecond).isEqualTo(limit - 2);
    }

    // ── 429 Too Many Requests ────────────────────────────────────────────

    @Test
    void comparisons_returns429_afterExceedingLimit() throws Exception {
        String body = """
                {"productIds": ["MLA2001234567", "MLA2009876543"]}
                """;

        // Comparisons limit = 2 en este test
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/comparisons")
                            .header("X-Forwarded-For", "10.0.0.50")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        // Tercer request: debe ser 429
        mockMvc.perform(post("/api/comparisons")
                        .header("X-Forwarded-For", "10.0.0.50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.path").value("/api/comparisons"));
    }

    @Test
    void diffEndpoint_sharesRateLimitBucketWithComparisons() throws Exception {
        String body = """
                {"productIds": ["MLA2001234567", "MLA2009876543"]}
                """;

        // Comparisons limit = 2; usar 2 en /comparisons
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/comparisons")
                            .header("X-Forwarded-For", "10.0.0.51")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        // /comparisons/diff comparte bucket → debe ser 429
        mockMvc.perform(post("/api/comparisons/diff")
                        .header("X-Forwarded-For", "10.0.0.51")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void search_returns429_afterExceedingSearchLimit() throws Exception {
        // Search limit = 3 en este test
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/products/search")
                            .header("X-Forwarded-For", "10.0.0.60")
                            .param("categoryId", "2")
                            .param("q", "Samsung"))
                    .andExpect(status().isOk());
        }

        // Cuarto request: debe ser 429
        mockMvc.perform(get("/api/products/search")
                        .header("X-Forwarded-For", "10.0.0.60")
                        .param("categoryId", "2")
                        .param("q", "Samsung"))
                .andExpect(status().isTooManyRequests());
    }

    // ── Buckets independientes por IP ────────────────────────────────────

    @Test
    void differentIps_haveIndependentBuckets() throws Exception {
        String body = """
                {"productIds": ["MLA2001234567", "MLA2009876543"]}
                """;

        // IP-A agota su límite (2)
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/comparisons")
                            .header("X-Forwarded-For", "10.0.0.70")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        // IP-A está bloqueada
        mockMvc.perform(post("/api/comparisons")
                        .header("X-Forwarded-For", "10.0.0.70")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());

        // IP-B sigue disponible
        mockMvc.perform(post("/api/comparisons")
                        .header("X-Forwarded-For", "10.0.0.71")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // ── Buckets independientes por tipo de endpoint ──────────────────────

    @Test
    void comparisonBucket_doesNotAffectDefaultBucket() throws Exception {
        String body = """
                {"productIds": ["MLA2001234567", "MLA2009876543"]}
                """;

        // Agotar comparisons (2)
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/comparisons")
                            .header("X-Forwarded-For", "10.0.0.80")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        // Comparisons bloqueado
        mockMvc.perform(post("/api/comparisons")
                        .header("X-Forwarded-For", "10.0.0.80")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());

        // Pero categories (bucket default) sigue accesible
        mockMvc.perform(get("/api/categories")
                        .header("X-Forwarded-For", "10.0.0.80"))
                .andExpect(status().isOk());
    }

    // ── No aplica a endpoints fuera de /api/ ─────────────────────────────

    @Test
    void nonApiEndpoints_areNotRateLimited() throws Exception {
        // Actuator y swagger no están bajo /api/ → no aplica rate limit
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-RateLimit-Limit"));
    }
}
