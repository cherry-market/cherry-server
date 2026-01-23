package com.cherry.server.product.repository;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
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

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.seller s " +
           "LEFT JOIN FETCH p.category c " +
           "WHERE (:status IS NULL OR p.status = :status) " +
           "AND (:tradeType IS NULL OR p.tradeType = :tradeType) " +
           "AND (:categoryId IS NULL OR (p.category IS NOT NULL AND p.category.id = :categoryId)) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:cursorCreatedAt IS NULL OR p.createdAt < :cursorCreatedAt " +
           "OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)) " +
           "ORDER BY p.createdAt DESC, p.id DESC")
    Slice<Product> findAllByCursorWithFilters(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("status") ProductStatus status,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("tradeType") TradeType tradeType,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.seller s " +
           "LEFT JOIN FETCH p.category c " +
           "WHERE p.id IN :ids")
    java.util.List<Product> findAllByIdInWithSellerAndCategory(@Param("ids") java.util.List<Long> ids);
}
