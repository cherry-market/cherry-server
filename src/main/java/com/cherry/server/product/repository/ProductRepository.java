package com.cherry.server.product.repository;

import com.cherry.server.product.domain.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    Slice<Product> findBySellerIdOrderByCreatedAtDescIdDesc(Long sellerId, Pageable pageable);

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.seller s " +
           "LEFT JOIN FETCH p.category c " +
           "WHERE p.seller.id = :sellerId " +
           "AND (p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)) " +
           "ORDER BY p.createdAt DESC, p.id DESC")
    Slice<Product> findBySellerIdWithCursor(@Param("sellerId") Long sellerId,
                                            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                            @Param("cursorId") Long cursorId,
                                            Pageable pageable);

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
           "WHERE p.id IN :ids")
    java.util.List<Product> findAllByIdInWithSellerAndCategory(@Param("ids") java.util.List<Long> ids);
}
