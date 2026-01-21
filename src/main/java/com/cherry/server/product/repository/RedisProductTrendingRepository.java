package com.cherry.server.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisProductTrendingRepository implements ProductTrendingRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String TRENDING_KEY = "trending:views:24h";
    private static final long TRENDING_TTL_HOURS = 24;

    @Override
    public void incrementViewCount(Long productId) {
        redisTemplate.opsForZSet().incrementScore(TRENDING_KEY, productId.toString(), 1);
        redisTemplate.expire(TRENDING_KEY, TRENDING_TTL_HOURS, TimeUnit.HOURS);
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
