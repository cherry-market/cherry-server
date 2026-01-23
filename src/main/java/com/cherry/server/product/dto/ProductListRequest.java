package com.cherry.server.product.dto;

import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import lombok.Builder;

@Builder
public record ProductListRequest(
        ProductStatus status,
        String categoryCode,
        Integer minPrice,
        Integer maxPrice,
        TradeType tradeType,
        ProductSortBy sortBy
) {
    public ProductSortBy sortByOrDefault() {
        return sortBy == null ? ProductSortBy.LATEST : sortBy;
    }
}
