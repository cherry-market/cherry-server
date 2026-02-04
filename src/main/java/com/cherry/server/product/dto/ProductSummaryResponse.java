package com.cherry.server.product.dto;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import java.util.List;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProductSummaryResponse(
        Long id,
        String title,
        int price,
        ProductStatus status,
        TradeType tradeType,
        String thumbnailUrl, // TODO: Image implementation
        CategoryResponse category,
        ProductDetailResponse.SellerResponse seller,
        LocalDateTime createdAt,
        List<String> tags,
        boolean isLiked,
        long likeCount
) {
    public static ProductSummaryResponse from(Product product) {
        return from(product, false, 0L, List.of());
    }

    public static ProductSummaryResponse from(Product product, boolean isLiked) {
        return from(product, isLiked, 0L, List.of());
    }

    public static ProductSummaryResponse from(Product product, boolean isLiked, long likeCount) {
        return from(product, isLiked, likeCount, List.of());
    }

    public static ProductSummaryResponse from(Product product, boolean isLiked, long likeCount, List<String> tags) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .status(product.getStatus())
                .tradeType(product.getTradeType())
                .thumbnailUrl(product.getImages().stream()
                        .filter(img -> img.isThumbnail())
                        .findFirst()
                        .map(img -> img.getThumbnailUrl() != null ? img.getThumbnailUrl() : img.getImageUrl())
                        .orElse(null))
                .category(CategoryResponse.from(product.getCategory()))
                .seller(new ProductDetailResponse.SellerResponse(
                        product.getSeller().getId(),
                        product.getSeller().getNickname()
                ))
                .createdAt(product.getCreatedAt())
                .tags(tags)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }
}
