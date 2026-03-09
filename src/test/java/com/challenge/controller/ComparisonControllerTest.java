package com.challenge.controller;

import com.challenge.dto.ComparisonDTO;
import com.challenge.dto.ComparisonDiffDTO;
import com.challenge.dto.ComparisonRequestDTO;
import com.challenge.exception.CategoryMismatchException;
import com.challenge.service.ComparisonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ComparisonController.class)
class ComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComparisonService comparisonService;

    @Test
    void compare_returns400_whenProductIdsListHasOnlyOneElement() throws Exception {
        String body = """
                {"productIds": ["MLA2001234567"]}
                """;

        mockMvc.perform(post("/api/comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void compare_returns400_whenProductIdsFieldIsMissing() throws Exception {
        mockMvc.perform(post("/api/comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void compare_returns200_withValidRequest() throws Exception {
        ComparisonDTO response = new ComparisonDTO(List.of(), List.of(), List.of());
        when(comparisonService.compare(any())).thenReturn(response);

        ComparisonRequestDTO request = new ComparisonRequestDTO(
                List.of("MLA2001234567", "MLA2009876543"), null);

        mockMvc.perform(post("/api/comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.attributeGroups").isArray())
                .andExpect(jsonPath("$.missingAttributes").isArray());
    }

    @Test
    void diff_returns200_withDiffResponse() throws Exception {
        ComparisonDiffDTO diffResponse = new ComparisonDiffDTO(List.of(), List.of());
        when(comparisonService.diff(any())).thenReturn(diffResponse);

        ComparisonRequestDTO request = new ComparisonRequestDTO(
                List.of("MLA2001234567", "MLA2009876543"), null);

        mockMvc.perform(post("/api/comparisons/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.attributeGroups").isArray());
    }

    @Test
    void diff_returns400_whenProductIdsHasOnlyOneElement() throws Exception {
        String body = """
                {"productIds": ["MLA2001234567"]}
                """;

        mockMvc.perform(post("/api/comparisons/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void compare_returns422_whenCategoriesAreNotComparable() throws Exception {
        when(comparisonService.compare(any()))
                .thenThrow(CategoryMismatchException.forProducts("2", "4"));

        ComparisonRequestDTO request = new ComparisonRequestDTO(
                List.of("MLA2001234567", "MLA9274837857"), null);

        mockMvc.perform(post("/api/comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void compare_returns400_whenFocusedAttributeIdsExceedsMax() throws Exception {
        List<Long> tooMany = new java.util.ArrayList<>();
        for (long i = 1; i <= 51; i++) tooMany.add(i);
        ComparisonRequestDTO request = new ComparisonRequestDTO(
                List.of("MLA2001234567", "MLA2009876543"), tooMany);

        mockMvc.perform(post("/api/comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
