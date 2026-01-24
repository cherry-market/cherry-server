package com.cherry.server.wish.service;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.dto.ProductSummaryResponse;
import com.cherry.server.product.dto.ProductListResponse;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.user.domain.User;
import com.cherry.server.user.repository.UserRepository;
import com.cherry.server.wish.domain.ProductLike;
import com.cherry.server.wish.repository.ProductLikeRepository;
import com.cherry.server.wish.repository.ProductLikeRepository.ProductLikeCount;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WishService {

    private final ProductLikeRepository productLikeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String LIKES_LIST_CACHE_PREFIX = "likes:list";
    private static final String PRODUCT_LIST_CACHE_PREFIX = "products:list";
    private static final long LIKES_LIST_CACHE_TTL_SECONDS = 300;

    public void addLike(Long userId, Long productId) {
        User user = getUser(userId);
        Product product = getProduct(productId);

        if (productLikeRepository.existsByUserAndProduct(user, product)) {
            return;
        }

        try {
            productLikeRepository.save(ProductLike.create(user, product));
            invalidateLikesCache(userId);
            invalidateProductListCache();
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    public void removeLike(Long userId, Long productId) {
        User user = getUser(userId);
        Product product = getProduct(productId);
        productLikeRepository.deleteByUserAndProduct(user, product);
        invalidateLikesCache(userId);
        invalidateProductListCache();
    }

    @Transactional(readOnly = true)
    public ProductListResponse getMyLikes(Long userId, String cursor, int limit) {
        getUser(userId);
        String cacheKey = buildLikesCacheKey(userId, cursor, limit);
        ProductListResponse cached = getCachedLikes(cacheKey);
        if (cached != null) {
            return cached;
        }
        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;

        if (cursor != null) {
            try {
                String[] parts = cursor.split("_");
                cursorCreatedAt = LocalDateTime.parse(parts[0]);
                cursorId = Long.parseLong(parts[1]);
            } catch (Exception ignored) {
            }
        }

        Slice<ProductLike> likes = productLikeRepository.findAllByUserIdWithProductCursor(
                userId,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, limit)
        );
        List<ProductLike> likeList = likes.getContent();
        List<Long> productIds = likeList.stream()
                .map(like -> like.getProduct().getId())
                .toList();
        Map<Long, Long> likeCountMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productLikeRepository.countByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductLikeCount::getProductId, ProductLikeCount::getLikeCount));
        List<ProductSummaryResponse> items = likeList.stream()
                .map(like -> ProductSummaryResponse.from(
                        like.getProduct(),
                        true,
                        likeCountMap.getOrDefault(like.getProduct().getId(), 0L)
                ))
                .toList();

        String nextCursor = null;
        if (likes.hasNext()) {
            ProductLike last = likes.getContent().get(likes.getContent().size() - 1);
            nextCursor = last.getCreatedAt().toString() + "_" + last.getId();
        }

        ProductListResponse response = new ProductListResponse(items, nextCursor);
        cacheLikes(cacheKey, response);
        return response;
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, Long productId) {
        User user = getUser(userId);
        Product product = getProduct(productId);
        return productLikeRepository.existsByUserAndProduct(user, product);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    private String buildLikesCacheKey(Long userId, String cursor, int limit) {
        return String.join(":",
                LIKES_LIST_CACHE_PREFIX,
                String.valueOf(userId),
                valueOf(cursor),
                Integer.toString(limit)
        );
    }

    private ProductListResponse getCachedLikes(String cacheKey) {
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue == null) {
                return null;
            }
            return objectMapper.readValue(cachedValue, ProductListResponse.class);
        } catch (Exception e) {
            log.debug("Failed to read likes cache", e);
            return null;
        }
    }

    private void cacheLikes(String cacheKey, ProductListResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, payload, LIKES_LIST_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Failed to write likes cache", e);
        }
    }

    private void invalidateLikesCache(Long userId) {
        deleteKeysByPattern(LIKES_LIST_CACHE_PREFIX + ":" + userId + ":*");
    }

    private void invalidateProductListCache() {
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

    private String valueOf(Object value) {
        return value == null ? "null" : value.toString();
    }
}
