package com.challenge.config;

import com.challenge.repository.CategoryAttributeRuleRepository;
import com.challenge.repository.CategoryRepository;
import com.challenge.repository.ProductRepository;
import com.challenge.service.CategoryService;
import com.challenge.service.ProductService;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifica que Spring Cache (Caffeine) funciona correctamente a nivel de proxy.
 * Usa @SpyBean sobre los repositorios para contar invocaciones reales.
 */
@SpringBootTest
class CacheIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @SpyBean
    private CategoryRepository categoryRepository;

    @SpyBean
    private CategoryAttributeRuleRepository attributeRuleRepository;

    @SpyBean
    private ProductRepository productRepository;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear());
        reset(categoryRepository, attributeRuleRepository, productRepository);
    }

    // ── categoryTree ────────────────────────────────────────────────────

    @Test
    @DisplayName("categoryTree: segunda llamada no toca el repository")
    void categoryTree_isCached() {
        categoryService.getTree();
        categoryService.getTree();

        verify(categoryRepository, times(1)).findAllWithChildren();
    }

    // ── categoryDetail ──────────────────────────────────────────────────

    @Test
    @DisplayName("categoryDetail: mismo id se cachea")
    void categoryDetail_isCachedById() {
        categoryService.getDetail(1L);
        categoryService.getDetail(1L);

        verify(categoryRepository, times(1)).findByIdWithParent(1L);
    }

    @Test
    @DisplayName("categoryDetail: ids distintos se cachean independientemente")
    void categoryDetail_differentIds_cachedIndependently() {
        categoryService.getDetail(1L);
        categoryService.getDetail(2L);

        verify(categoryRepository, times(1)).findByIdWithParent(1L);
        verify(categoryRepository, times(1)).findByIdWithParent(2L);
    }

    // ── categoryAttributes ──────────────────────────────────────────────

    @Test
    @DisplayName("categoryAttributes: segunda llamada no toca el repository")
    void categoryAttributes_isCached() {
        categoryService.getAttributeGroups(1L);
        categoryService.getAttributeGroups(1L);

        verify(attributeRuleRepository, times(1)).findByCategoryId(1L);
    }

    // ── productDetail ───────────────────────────────────────────────────

    @Test
    @DisplayName("productDetail: segunda llamada no toca el repository")
    void productDetail_isCached() {
        productService.getDetail("MLA2001234567");
        productService.getDetail("MLA2001234567");

        verify(productRepository, times(1)).findWithRelationsById("MLA2001234567");
    }

    // ── CacheManager ────────────────────────────────────────────────────

    @Test
    @DisplayName("CacheManager contiene los 5 caches configurados")
    void cacheManager_hasAllExpectedCaches() {
        assertThat(cacheManager.getCacheNames())
                .contains("categoryTree", "categoryDetail",
                        "categoryAttributes", "productDetail",
                        "exchangeRates");
    }

    @Test
    @DisplayName("CacheManager es de tipo Caffeine")
    void cacheManager_isCaffeineBased() {
        assertThat(cacheManager).isInstanceOf(
                org.springframework.cache.caffeine.CaffeineCacheManager.class);
    }
}
