package com.cherry.server.product.dto;

import java.util.List;

public record ProductListResponse(
        List<ProductSummaryResponse> items,
        String nextCursor
) {
}
