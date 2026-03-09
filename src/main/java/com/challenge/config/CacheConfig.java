package com.challenge.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de Spring Cache con Caffeine como proveedor local.
 * @see org.springframework.cache.annotation.Cacheable
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);
        manager.setCaffeine(defaultCaffeineBuilder());
        manager.registerCustomCache("categoryTree",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(1)
                        .build());
        manager.registerCustomCache("categoryDetail",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(50)
                        .build());
        manager.registerCustomCache("categoryAttributes",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(50)
                        .build());
        manager.registerCustomCache("productDetail",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build());
        manager.registerCustomCache("exchangeRates",
                Caffeine.newBuilder()
                        .expireAfterWrite(12, TimeUnit.HOURS)
                        .maximumSize(20)
                        .build());
        return manager;
    }

    private Caffeine<Object, Object> defaultCaffeineBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(100);
    }
}
