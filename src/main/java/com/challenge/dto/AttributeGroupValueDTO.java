package com.challenge.dto;

import java.util.List;

public record AttributeGroupValueDTO(
        Long groupId,
        String groupName,
        List<AttributeValueDisplayDTO> attributes
        ) {

    public record AttributeValueDisplayDTO(
            String displayName,
            String displayValue
        ) {}
}
