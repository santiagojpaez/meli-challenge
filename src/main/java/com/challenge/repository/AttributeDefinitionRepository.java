package com.challenge.repository;

import com.challenge.model.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {

    List<AttributeDefinition> findDistinctByCategoryAttributeRules_Category_IdIn(List<Long> categoryIds);

    List<AttributeDefinition> findByProductFieldIsNotNull();
}
