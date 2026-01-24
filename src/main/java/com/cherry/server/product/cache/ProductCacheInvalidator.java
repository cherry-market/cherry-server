package com.cherry.server.product.cache;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCacheInvalidator {

    private static final String PRODUCT_LIST_CACHE_PREFIX = "products:list";

    private final StringRedisTemplate redisTemplate;

    public void invalidateProductListCache() {
        deleteKeysByPattern(PRODUCT_LIST_CACHE_PREFIX + ":*");
    }

    private void deleteKeysByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return;
            }
            redisTemplate.delete(keys);
        } catch (Exception e) {
            log.debug("Failed to delete cache keys for pattern {}", pattern, e);
        }
    }
}
