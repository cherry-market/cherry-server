package com.cherry.server.product.dto;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Builder
public record ProductDetailResponse(
        Long id,
        String title,
        int price,
        ProductStatus status,
        TradeType tradeType,
        List<String> imageUrls, // Dummy
        CategoryResponse category,
        List<String> tags,
        String description,
        SellerResponse seller,
        LocalDateTime createdAt,
        boolean isLiked,
        long likeCount
) {
    @Builder
    public record SellerResponse(Long id, String nickname) {}

    public static ProductDetailResponse from(Product product) {
        return from(product, false, 0L);
    }

    public static ProductDetailResponse from(Product product, boolean isLiked) {
        return from(product, isLiked, 0L);
    }

    public static ProductDetailResponse from(Product product, boolean isLiked, long likeCount) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .status(product.getStatus())
                .tradeType(product.getTradeType())
                .imageUrls(product.getImages().stream()
                        .filter(img -> !img.isThumbnail()) // 썸네일 제외
                        .sorted(Comparator.comparingInt(img -> img.getImageOrder()))
                .map(img -> img.getImageUrl())
                .toList())
                .category(CategoryResponse.from(product.getCategory()))
                .tags(product.getProductTags().stream()
                        .map(pt -> pt.getTag().getName())
                        .toList())
                .description(product.getDescription())
                .seller(new SellerResponse(product.getSeller().getId(), product.getSeller().getNickname()))
                .createdAt(product.getCreatedAt())
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }
}
