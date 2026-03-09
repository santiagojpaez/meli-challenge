package com.challenge.repository;

import com.challenge.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

    @Query("SELECT pa FROM ProductAttribute pa " +
            "JOIN FETCH pa.attributeDefinition " +
            "WHERE pa.product.id = :productId")
    List<ProductAttribute> findByProductId(@Param("productId") String productId);

    @Query("SELECT pa FROM ProductAttribute pa " +
            "JOIN FETCH pa.attributeDefinition ad " +
            "WHERE pa.product.id IN :productIds")
    List<ProductAttribute> findByProductIdIn(@Param("productIds") List<String> productIds);

    @Query("SELECT pa FROM ProductAttribute pa " +
            "JOIN FETCH pa.attributeDefinition ad " +
            "WHERE pa.product.id IN :productIds AND pa.attributeDefinition.id IN :attributeDefIds")
    List<ProductAttribute> findByProductIdInAndAttributeDefIdIn(
            @Param("productIds") List<String> productIds,
            @Param("attributeDefIds") List<Long> attributeDefIds);

    @Query("SELECT pa.product.id FROM ProductAttribute pa " +
            "WHERE pa.attributeDefinition.canonicalName = :attributeName " +
            "AND pa.normalizedValue BETWEEN :min AND :max")
    List<String> findProductIdsByAttributeRange(
            @Param("attributeName") String attributeName,
            @Param("min") BigDecimal min,
            @Param("max") BigDecimal max);
}
