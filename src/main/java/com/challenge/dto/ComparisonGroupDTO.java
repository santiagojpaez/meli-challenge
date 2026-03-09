package com.challenge.dto;

import java.util.List;

public record ComparisonGroupDTO(
        String groupName,
        int groupOrder,
        List<ComparisonAttributeDTO> attributes
) {}
