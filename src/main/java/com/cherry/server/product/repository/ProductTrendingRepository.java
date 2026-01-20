package com.cherry.server.product.repository;

import java.util.List;

public interface ProductTrendingRepository {
    void incrementViewCount(Long productId);
    List<Long> getTopTrendingProductIds(int limit);
}
