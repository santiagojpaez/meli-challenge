package com.challenge.repository;

import com.challenge.model.ComparableCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComparableCategoryRepository extends JpaRepository<ComparableCategory, Long> {

    @Query("SELECT cc FROM ComparableCategory cc "
            + "JOIN FETCH cc.categoryA JOIN FETCH cc.categoryB "
            + "WHERE cc.categoryA.id = :categoryId OR cc.categoryB.id = :categoryId")
    List<ComparableCategory> findAllByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT cc FROM ComparableCategory cc "
            + "JOIN FETCH cc.categoryA JOIN FETCH cc.categoryB "
            + "WHERE (cc.categoryA.id = :categoryIdA AND cc.categoryB.id = :categoryIdB) "
            + "OR (cc.categoryA.id = :categoryIdB AND cc.categoryB.id = :categoryIdA)")
    Optional<ComparableCategory> findPairByCategoryIds(
            @Param("categoryIdA") Long categoryIdA,
            @Param("categoryIdB") Long categoryIdB);
}
