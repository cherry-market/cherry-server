package com.cherry.server.wish.repository;

import com.cherry.server.user.domain.User;
import com.cherry.server.product.domain.Product;
import com.cherry.server.wish.domain.ProductLike;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {

    boolean existsByUserAndProduct(User user, Product product);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserAndProduct(User user, Product product);

    Page<ProductLike> findAllByUserId(Long userId, Pageable pageable);

    long countByProductId(Long productId);

    @Query("SELECT pl.product.id as productId, COUNT(pl) as likeCount " +
            "FROM ProductLike pl " +
            "WHERE pl.product.id IN :productIds " +
            "GROUP BY pl.product.id")
    List<ProductLikeCount> countByProductIds(@Param("productIds") List<Long> productIds);

    @Query("SELECT pl FROM ProductLike pl " +
            "JOIN FETCH pl.product " +
            "WHERE pl.user.id = :userId " +
            "AND (:cursorCreatedAt IS NULL OR pl.createdAt < :cursorCreatedAt " +
            "OR (pl.createdAt = :cursorCreatedAt AND pl.id < :cursorId)) " +
            "ORDER BY pl.createdAt DESC, pl.id DESC")
    Slice<ProductLike> findAllByUserIdWithProductCursor(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT pl.product.id FROM ProductLike pl WHERE pl.user.id = :userId AND pl.product.id IN :productIds")
    List<Long> findLikedProductIds(
            @Param("userId") Long userId,
            @Param("productIds") List<Long> productIds
    );

    interface ProductLikeCount {
        Long getProductId();
        Long getLikeCount();
    }
}
