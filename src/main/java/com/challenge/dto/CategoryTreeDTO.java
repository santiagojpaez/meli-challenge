package com.challenge.dto;

import org.springframework.lang.Nullable;

import java.util.List;

public record CategoryTreeDTO(
        Long id,
        String name,
        List<CategoryTreeDTO> children
) {}
