package com.cherry.server.product.repository;

import com.cherry.server.product.domain.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Cursor Pagination: (createdAt < lastCreatedAt) OR (createdAt == lastCreatedAt AND id < lastId)
    // Ordered by createdAt DESC, id DESC
    @Query("SELECT p FROM Product p " +
           "WHERE (:cursorCreatedAt IS NULL OR p.createdAt < :cursorCreatedAt " +
           "OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)) " +
           "ORDER BY p.createdAt DESC, p.id DESC")
    Slice<Product> findAllByCursor(@Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                   @Param("cursorId") Long cursorId,
                                   Pageable pageable);
}
