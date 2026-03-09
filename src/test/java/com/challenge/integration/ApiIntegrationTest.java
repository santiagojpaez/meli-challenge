package com.challenge.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración que verifican el flujo completo de la aplicación:
 * controller → service → repository → H2 en memoria cargado con data.sql.
 *
 * Se deshabilita la clave de la API de tipo de cambio para que los tests
 * sean deterministas y no dependan de servicios externos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "exchange.rate.api.key=")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCategories_returnsRealRootCategoriesFromDatabase() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Tecnología')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Electrodomésticos')]").exists());
    }

    @Test
    void getProductDetail_returnsRealProductFromDatabase() throws Exception {
        mockMvc.perform(get("/api/products/MLA2001234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productSummary.id").value("MLA2001234567"))
                .andExpect(jsonPath("$.productSummary.condition").value("NEW"))
                .andExpect(jsonPath("$.productSummary.price.currency").value("ARS"))
                .andExpect(jsonPath("$.category.name").value("Smartphones"));
    }

    @Test
    void compareProducts_returnsFullComparisonForSameCategoryProducts() throws Exception {
      // Compara dos smartphones de la misma categoría (ambos con precio en ARS)
        String body = """
                {"productIds": ["MLA2001234567", "MLA2009876543"]}
                """;

        mockMvc.perform(post("/api/comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()").value(2))
                .andExpect(jsonPath("$.attributeGroups").isArray());
    }

    @Test
    void compareProducts_returns422_whenCategoriesAreNotComparable() throws Exception {
        // MLA2001234567 = iPhone (Smartphones), MLA9274837857 = Cafetera Oster – no son comparables
        String body = """
                {"productIds": ["MLA2001234567", "MLA9274837857"]}
                """;

        mockMvc.perform(post("/api/comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }
}
