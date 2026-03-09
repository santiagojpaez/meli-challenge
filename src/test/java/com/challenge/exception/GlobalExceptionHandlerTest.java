package com.challenge.exception;

import com.challenge.dto.ComparisonRequestDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc test for GlobalExceptionHandler.
 * Uses a small fake controller to trigger each exception type.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleItemNotFound_returns404WithErrorBody() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void handleCategoryMismatch_returns422WithErrorBody() throws Exception {
        mockMvc.perform(get("/test/mismatch"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"));
    }

    @Test
    void handleInvalidComparisonRequest_returns400WithErrorBody() throws Exception {
        mockMvc.perform(get("/test/invalid-comparison"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() throws Exception {
        mockMvc.perform(get("/test/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("bad argument"));
    }

    @Test
    void handleConstraintViolation_returns400WithDetails() throws Exception {
        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("validación")));
    }

    @Test
    void handleMethodArgumentNotValid_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("validación")));
    }

    @Test
    void handleHttpMessageNotReadable_returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/test/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ malformed json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void handleGenericException_returns500WithSafeMessage() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                // Must not expose internal details
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("NullPointerException"))));
    }

    // ── Internal fake controller ─────────────────────────────────────────────

    @RestController
    @RequestMapping("/test")
    static class FakeController {

        @GetMapping("/not-found")
        public void notFound() {
            throw ItemNotFoundException.forProduct("P99");
        }

        @GetMapping("/mismatch")
        public void mismatch() {
            throw CategoryMismatchException.forProducts("1", "2");
        }

        @GetMapping("/invalid-comparison")
        public void invalidComparison() {
            throw InvalidComparisonRequestException.sameProduct();
        }

        @GetMapping("/illegal-arg")
        public void illegalArg() {
            throw new IllegalArgumentException("bad argument");
        }

        @GetMapping("/constraint")
        public void constraint() {
            throw new ConstraintViolationException("constraint msg", Set.of());
        }

        @GetMapping("/error")
        public void error() {
            throw new NullPointerException("secret internal detail");
        }

        @PostMapping("/valid")
        public String valid(@Valid @RequestBody ValidDTO dto) {
            return "ok";
        }

        record ValidDTO(@NotBlank String name) {}
    }
}
