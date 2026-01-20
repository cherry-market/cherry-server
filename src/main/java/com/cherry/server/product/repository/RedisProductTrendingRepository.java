package com.cherry.server.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisProductTrendingRepository implements ProductTrendingRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String TRENDING_KEY = "trending:views:24h";

    @Override
    public void incrementViewCount(Long productId) {
        redisTemplate.opsForZSet().incrementScore(TRENDING_KEY, productId.toString(), 1);
    }

    @Override
    public List<Long> getTopTrendingProductIds(int limit) {
        Set<String> productIds = redisTemplate.opsForZSet()
                .reverseRange(TRENDING_KEY, 0, limit - 1);

        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        return productIds.stream()
                .map(Long::parseLong)
                .toList();
    }
}
