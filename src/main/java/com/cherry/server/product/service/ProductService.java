package com.cherry.server.product.service;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.dto.ProductDetailResponse;
import com.cherry.server.product.dto.ProductListResponse;
import com.cherry.server.product.dto.ProductSummaryResponse;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.product.repository.ProductTrendingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductTrendingRepository productTrendingRepository;

    public ProductListResponse getProducts(String cursor, int limit) {
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

        Slice<Product> slice = productRepository.findAllByCursor(cursorCreatedAt, cursorId, PageRequest.of(0, limit));
        List<ProductSummaryResponse> items = slice.getContent().stream()
                .map(ProductSummaryResponse::from)
                .toList();
        
        String nextCursor = null;
        if (slice.hasNext()) {
            Product last = slice.getContent().get(slice.getContent().size() - 1);
            nextCursor = last.getCreatedAt().toString() + "_" + last.getId();
        }

        return new ProductListResponse(items, nextCursor);
    }

    @Transactional
    public ProductDetailResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        // Async increment view count
        productTrendingRepository.incrementViewCount(productId);

        return ProductDetailResponse.from(product);
    }
    
    public ProductListResponse getTrending() {
        List<Long> topIds = productTrendingRepository.getTopTrendingProductIds(10);
        
        if (topIds.isEmpty()) {
            return new ProductListResponse(Collections.emptyList(), null);
        }

        List<Product> products = productRepository.findAllById(topIds);
        
        // Map for O(1) Access
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Improve Sorting: O(N) instead of nested stream (O(N^2))
        List<ProductSummaryResponse> items = topIds.stream()
                .filter(productMap::containsKey)
                .map(productMap::get)
                .map(ProductSummaryResponse::from)
                .toList();

        return new ProductListResponse(items, null);
    }
}
