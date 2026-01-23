package com.cherry.server.product.service;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.dto.ProductDetailResponse;
import com.cherry.server.product.dto.ProductListResponse;
import com.cherry.server.product.dto.ProductSearchCondition;
import com.cherry.server.product.dto.ProductSummaryResponse;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.product.repository.ProductTrendingRepository;
import com.cherry.server.wish.repository.ProductLikeRepository;
import com.cherry.server.wish.repository.ProductLikeRepository.ProductLikeCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductTrendingRepository productTrendingRepository;
    private final ProductLikeRepository productLikeRepository;

    public ProductListResponse getProducts(String cursor, int limit, Long userId, ProductSearchCondition condition) {
        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;

        if (cursor != null) {
            // Decoded dummy logic for P0: "timestamp_id"
            try {
                String[] parts = cursor.split("_");
                cursorCreatedAt = LocalDateTime.parse(parts[0]);
                cursorId = Long.parseLong(parts[1]);
            } catch (Exception e) {
                // Invalid cursor, treat as first page
            }
        }

        Slice<Product> slice = productRepository.findAllByCursorWithFilters(
                cursorCreatedAt,
                cursorId,
                condition.status(),
                condition.categoryId(),
                condition.minPrice(),
                condition.maxPrice(),
                condition.tradeType(),
                PageRequest.of(0, limit)
        );
        List<Product> products = slice.getContent();
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();
        Set<Long> likedProductIds = userId == null || productIds.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(productLikeRepository.findLikedProductIds(userId, productIds));
        Map<Long, Long> likeCountMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productLikeRepository.countByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductLikeCount::getProductId, ProductLikeCount::getLikeCount));
        List<ProductSummaryResponse> items = products.stream()
                .map(product -> ProductSummaryResponse.from(
                        product,
                        likedProductIds.contains(product.getId()),
                        likeCountMap.getOrDefault(product.getId(), 0L)
                ))
                .toList();
        
        String nextCursor = null;
        if (slice.hasNext()) {
            Product last = slice.getContent().get(slice.getContent().size() - 1);
            nextCursor = last.getCreatedAt().toString() + "_" + last.getId();
        }

        return new ProductListResponse(items, nextCursor);
    }

    @Transactional
    public ProductDetailResponse getProduct(Long productId, Long userId) {
        // DB에서 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        // Async increment view count
        // 조회수 증가 (Redis에 비동기 저장)
        productTrendingRepository.incrementViewCount(productId);

        // DTO로 변환하여 반환
        boolean isLiked = userId != null && productLikeRepository.existsByUserIdAndProductId(userId, productId);
        long likeCount = productLikeRepository.countByProductId(productId);
        return ProductDetailResponse.from(product, isLiked, likeCount);
    }
    
    public ProductListResponse getTrending(Long userId) {
        List<Long> topIds = productTrendingRepository.getTopTrendingProductIds(10);
        
        if (topIds.isEmpty()) {
            return new ProductListResponse(Collections.emptyList(), null);
        }

        List<Product> products = productRepository.findAllByIdInWithSellerAndCategory(topIds);
        
        // Map for O(1) Access
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Improve Sorting: O(N) instead of nested stream (O(N^2))
        Set<Long> likedProductIds = userId == null
                ? Collections.emptySet()
                : new HashSet<>(productLikeRepository.findLikedProductIds(userId, topIds));
        Map<Long, Long> likeCountMap = productLikeRepository.countByProductIds(topIds).stream()
                .collect(Collectors.toMap(ProductLikeCount::getProductId, ProductLikeCount::getLikeCount));
        List<ProductSummaryResponse> items = topIds.stream()
                .filter(productMap::containsKey)
                .map(productMap::get)
                .map(product -> ProductSummaryResponse.from(
                        product,
                        likedProductIds.contains(product.getId()),
                        likeCountMap.getOrDefault(product.getId(), 0L)
                ))
                .toList();

        return new ProductListResponse(items, null);
    }
}
