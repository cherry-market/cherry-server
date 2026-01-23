package com.cherry.server.product.dto;

import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;

public record ProductSearchCondition(
        ProductStatus status,
        String categoryCode,
        Integer minPrice,
        Integer maxPrice,
        TradeType tradeType,
        ProductSortBy sortBy
) {
}
