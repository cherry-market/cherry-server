package com.cherry.server.product.dto;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
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
        LocalDateTime createdAt
) {
    public static ProductSummaryResponse from(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .status(product.getStatus())
                .tradeType(product.getTradeType())
                .thumbnailUrl("https://via.placeholder.com/150") // Dummy for P0
                .createdAt(product.getCreatedAt())
                .build();
    }
}
