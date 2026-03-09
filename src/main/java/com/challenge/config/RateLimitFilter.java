package com.challenge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.lang.NonNull;

/**
 * Filtro de rate limiting por IP usando ventana fija (fixed window).
 * Responde 429 Too Many Requests con header Retry-After cuando se excede el límite.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final int comparisonLimit;
    private final int searchLimit;
    private final int defaultLimit;
    private final long windowMs;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RateLimitFilter(
            @Value("${rate-limit.comparisons-per-minute:20}") int comparisonLimit,
            @Value("${rate-limit.search-per-minute:60}") int searchLimit,
            @Value("${rate-limit.default-per-minute:100}") int defaultLimit,
            @Value("${rate-limit.window-ms:60000}") long windowMs) {
        this.comparisonLimit = comparisonLimit;
        this.searchLimit = searchLimit;
        this.defaultLimit = defaultLimit;
        this.windowMs = windowMs;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        int limit = resolveLimit(method, path);
        String bucketKey = clientIp + ":" + resolveBucketCategory(method, path);

        TokenBucket bucket = buckets.compute(bucketKey, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || existing.isExpired(now, windowMs)) {
                return new TokenBucket(now, new AtomicInteger(0));
            }
            return existing;
        });

        int current = bucket.counter().incrementAndGet();

        long remainingMs = windowMs - (System.currentTimeMillis() - bucket.windowStart());
        int retryAfterSeconds = Math.max(1, (int) Math.ceil(remainingMs / 1000.0));

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - current)));

        if (current > limit) {
            log.warn("Rate limit exceeded for IP {} on {} {} ({}/{})",
                    clientIp, method, path, current, limit);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

            Map<String, Object> error = Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Has excedido el límite de " + limit + " solicitudes por minuto. Reintentá en " + retryAfterSeconds + " segundos.",
                    "path", path
            );
            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimit(String method, String path) {
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/comparisons")) {
            return comparisonLimit;
        }
        if (path.startsWith("/api/products/search")) {
            return searchLimit;
        }
        return defaultLimit;
    }

    private String resolveBucketCategory(String method, String path) {
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/comparisons")) {
            return "comparisons";
        }
        if (path.startsWith("/api/products/search")) {
            return "search";
        }
        return "default";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record TokenBucket(long windowStart, AtomicInteger counter) {
        boolean isExpired(long now, long windowMs) {
            return now - windowStart >= windowMs;
        }
    }
}
