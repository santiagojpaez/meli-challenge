package com.challenge.controller;

import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.ProductDetailDTO;
import com.challenge.dto.ProductSummaryDTO;
import com.challenge.exception.ItemNotFoundException;
import com.challenge.model.ItemCondition;
import com.challenge.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void getDetail_returns200WithProductDetail() throws Exception {
        ProductSummaryDTO summary = new ProductSummaryDTO(
                "MLA2001234567",
                "Apple iPhone 15 Pro Max 256 GB Titanio Negro",
                "iPhone 15 Pro Max con chip A17 Pro.",
                ItemCondition.NEW,
                "https://example.com/iphone15.jpg",
                "Titanio Negro",
                4.8,
                new ProductSummaryDTO.PriceSummaryDTO(BigDecimal.valueOf(2199999), null, "ARS"),
                new ProductSummaryDTO.ShippingSummaryDTO(true, false)
        );
        ProductDetailDTO detail = new ProductDetailDTO(
                summary,
                BigDecimal.valueOf(221),
                "159.9 x 76.7 x 8.25 mm",
                45,
                312,
                new CategorySummaryDTO(2L, "Smartphones"),
                List.of()
        );

        when(productService.getDetail("MLA2001234567")).thenReturn(detail);

        mockMvc.perform(get("/api/products/MLA2001234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productSummary.id").value("MLA2001234567"))
                .andExpect(jsonPath("$.productSummary.name").value("Apple iPhone 15 Pro Max 256 GB Titanio Negro"))
                .andExpect(jsonPath("$.productSummary.condition").value("NEW"))
                .andExpect(jsonPath("$.availableQuantity").value(45))
                .andExpect(jsonPath("$.category.name").value("Smartphones"));
    }

    @Test
    void getDetail_returns404_whenProductNotFound() throws Exception {
        when(productService.getDetail("NONEXISTENT"))
                .thenThrow(ItemNotFoundException.forProduct("NONEXISTENT"));

        mockMvc.perform(get("/api/products/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void search_returns200WithPagedResults() throws Exception {
        ProductSummaryDTO summary = new ProductSummaryDTO(
                "MLA2001234567",
                "Apple iPhone 15 Pro Max",
                null,
                ItemCondition.NEW,
                null, null, 4.8,
                new ProductSummaryDTO.PriceSummaryDTO(BigDecimal.valueOf(2199999), null, "ARS"),
                null
        );
        Page<ProductSummaryDTO> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1);
        when(productService.search(eq(2L), eq("iphone"), any())).thenReturn(page);

        mockMvc.perform(get("/api/products/search")
                .param("categoryId", "2")
                .param("q", "iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("MLA2001234567"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_returns400_whenCategoryIdIsNegative() throws Exception {
        mockMvc.perform(get("/api/products/search")
                .param("categoryId", "-1")
                .param("q", "iphone"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returns400_whenQueryParamIsBlank() throws Exception {
        mockMvc.perform(get("/api/products/search")
                .param("categoryId", "2")
                .param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returns400_whenQueryExceeds200Characters() throws Exception {
        String longQ = "a".repeat(201);
        mockMvc.perform(get("/api/products/search")
                .param("categoryId", "2")
                .param("q", longQ))
                .andExpect(status().isBadRequest());
    }
}
