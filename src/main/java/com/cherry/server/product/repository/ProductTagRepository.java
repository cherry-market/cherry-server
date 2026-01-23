package com.cherry.server.product.repository;

import com.cherry.server.product.domain.ProductTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
    @Query("SELECT pt FROM ProductTag pt " +
           "JOIN FETCH pt.tag t " +
           "WHERE pt.product.id IN :productIds")
    List<ProductTag> findAllByProductIdInWithTag(@Param("productIds") List<Long> productIds);
}
