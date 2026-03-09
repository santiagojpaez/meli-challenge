package com.challenge.controller;

import com.challenge.dto.CategoryDetailDTO;
import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.CategoryTreeDTO;
import com.challenge.exception.ItemNotFoundException;
import com.challenge.service.CategoryService;
import com.challenge.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private ProductService productService;

    @Test
    void getTree_returns200WithRootCategoryList() throws Exception {
        when(categoryService.getTree()).thenReturn(List.of(
                new CategoryTreeDTO(1L, "Tecnología", List.of()),
                new CategoryTreeDTO(3L, "Electrodomésticos", List.of())
        ));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Tecnología"))
                .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    void getDetail_returns200_whenCategoryExists() throws Exception {
        CategoryDetailDTO detail = new CategoryDetailDTO(
                2L, "Smartphones",
                new CategorySummaryDTO(1L, "Tecnología"),
                List.of(),
                List.of()
        );
        when(categoryService.getDetail(2L)).thenReturn(detail);

        mockMvc.perform(get("/api/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Smartphones"))
                .andExpect(jsonPath("$.parent.name").value("Tecnología"));
    }

    @Test
    void getDetail_returns404_whenCategoryNotFound() throws Exception {
        when(categoryService.getDetail(99L))
                .thenThrow(ItemNotFoundException.forCategory("99"));

        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
