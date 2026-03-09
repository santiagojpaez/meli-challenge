package com.challenge.dto;

import java.util.List;

public record AttributeGroupDTO(
        Long groupId,
        String groupName,
        int displayOrder,
        List<AttributeRuleSummaryDTO> attributes
) {}
