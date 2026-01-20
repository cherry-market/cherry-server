package com.cherry.server.product.dto;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ProductDetailResponse(
        Long id,
        String title,
        int price,
        ProductStatus status,
        TradeType tradeType,
        List<String> imageUrls, // Dummy
        String description,
        SellerResponse seller,
        LocalDateTime createdAt
) {
    @Builder
    public record SellerResponse(Long id, String nickname) {}

    public static ProductDetailResponse from(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .status(product.getStatus())
                .tradeType(product.getTradeType())
                .imageUrls(List.of("https://via.placeholder.com/400")) // Dummy
                .description(product.getDescription())
                .seller(new SellerResponse(product.getSeller().getId(), product.getSeller().getNickname()))
                .createdAt(product.getCreatedAt())
                .build();
    }
}
