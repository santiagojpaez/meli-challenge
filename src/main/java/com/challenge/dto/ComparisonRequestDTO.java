package com.challenge.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.util.List;

public record ComparisonRequestDTO(
        @NotEmpty
        @Size(min = 2, max = 5, message = "Una comparación debe incluir entre 2 y 5 productos")
        List<String> productIds,

        @Nullable
        @Size(max = 50, message = "Se pueden enfocar como máximo 50 atributos")
        List<Long> focusedAttributeIds
) {}
