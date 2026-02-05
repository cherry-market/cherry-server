package com.cherry.server.product.repository;

import com.cherry.server.product.domain.ProductImage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    Optional<ProductImage> findByOriginalUrl(String originalUrl);

    long countByProductId(Long productId);

    long countByProductIdAndImageUrlIsNotNull(Long productId);
}
