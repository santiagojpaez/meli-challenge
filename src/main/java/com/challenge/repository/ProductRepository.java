package com.challenge.repository;

import com.challenge.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"category", "price", "shipping"})
    Optional<Product> findWithRelationsById(String id);

    @EntityGraph(attributePaths = {"price", "shipping"})
    Page<Product> findByCategory_Id(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"price", "shipping"})
    Page<Product> findByCategory_IdIn(List<Long> categoryIds, Pageable pageable);

    @Query(value = "SELECT DISTINCT p FROM Product p "
            + "JOIN FETCH p.price JOIN FETCH p.shipping "
            + "WHERE p.category.id IN :categoryIds "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR EXISTS (SELECT 1 FROM ProductAttribute pa WHERE pa.product = p "
            + "AND pa.attributeDefinition.canonicalName IN ('brand', 'model_name') "
            + "AND LOWER(pa.rawValue) LIKE LOWER(CONCAT('%', :query, '%'))))",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Product p "
            + "WHERE p.category.id IN :categoryIds "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR EXISTS (SELECT 1 FROM ProductAttribute pa WHERE pa.product = p "
            + "AND pa.attributeDefinition.canonicalName IN ('brand', 'model_name') "
            + "AND LOWER(pa.rawValue) LIKE LOWER(CONCAT('%', :query, '%'))))") 
    Page<Product> searchByText(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("query") String query,
            Pageable pageable);

    @EntityGraph(attributePaths = {"category", "price", "shipping"})
    List<Product> findAllByIdIn(List<String> ids);
}
