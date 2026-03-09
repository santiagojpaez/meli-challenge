package com.challenge.repository;

import com.challenge.model.CategoryAttributeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryAttributeRuleRepository extends JpaRepository<CategoryAttributeRule, Long> {

    @Query("SELECT car FROM CategoryAttributeRule car " +
            "JOIN FETCH car.attributeDefinition " +
            "JOIN FETCH car.attributeGroup " +
            "WHERE car.category.id = :categoryId")
    List<CategoryAttributeRule> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT car FROM CategoryAttributeRule car " +
            "JOIN FETCH car.attributeDefinition " +
            "JOIN FETCH car.attributeGroup " +
            "WHERE car.category.id IN :categoryIds")
    List<CategoryAttributeRule> findByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);


    @Query("SELECT car FROM CategoryAttributeRule car "
            + "JOIN FETCH car.attributeDefinition "
            + "JOIN FETCH car.attributeGroup "
            + "WHERE car.category.id = :categoryId AND car.isRequired = true")
    List<CategoryAttributeRule> findRequiredByCategoryId(@Param("categoryId") Long categoryId);
}
