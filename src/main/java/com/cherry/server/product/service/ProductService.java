package com.cherry.server.product.service;

import com.cherry.server.product.cache.ProductCacheInvalidator;
import com.cherry.server.product.domain.Category;
import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductImage;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.ProductTag;
import com.cherry.server.product.domain.Tag;
import com.cherry.server.product.dto.ProductCreateRequest;
import com.cherry.server.product.dto.ProductCreateResponse;
import com.cherry.server.product.dto.ProductDetailResponse;
import com.cherry.server.product.dto.ProductListResponse;
import com.cherry.server.product.dto.ProductSearchCondition;
import com.cherry.server.product.dto.ProductSortBy;
import com.cherry.server.product.dto.ProductSummaryResponse;
import com.cherry.server.product.repository.CategoryRepository;
import com.cherry.server.product.repository.ProductImageRepository;
import com.cherry.server.product.repository.ProductTagRepository;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.product.repository.ProductTrendingRepository;
import com.cherry.server.product.repository.TagRepository;
import com.cherry.server.upload.storage.StorageProperties;
import com.cherry.server.user.domain.User;
import com.cherry.server.user.repository.UserRepository;
import com.cherry.server.wish.repository.ProductLikeRepository;
import com.cherry.server.wish.repository.ProductLikeRepository.ProductLikeCount;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductTrendingRepository productTrendingRepository;
    private final ProductLikeRepository productLikeRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductImageRepository productImageRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductCacheInvalidator productCacheInvalidator;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final StorageProperties storageProperties;

    @Value("${storage.base-url:}")
    private String storageBaseUrl;

    private static final String PRODUCT_LIST_CACHE_PREFIX = "products:list";
    private static final long PRODUCT_LIST_CACHE_TTL_SECONDS = 300;

    public ProductListResponse getProducts(String cursor, int limit, Long userId, ProductSearchCondition condition, ProductSortBy sortBy) {
        String cacheKey = buildProductsCacheKey(cursor, condition, sortBy, limit);
        ProductListResponse cached = getCachedProductList(cacheKey);
        if (cached != null) {
            if (userId == null) {
                return cached;
            }
            return applyLikedFlags(cached, userId);
        }

        LocalDateTime cursorCreatedAt = null;
        Integer cursorPrice = null;
        Long cursorId = null;

        if (cursor != null) {
            try {
                int underscoreIndex = cursor.lastIndexOf('_');
                if (underscoreIndex <= 0 || underscoreIndex == cursor.length() - 1) {
                    throw new IllegalArgumentException("Invalid cursor");
                }
                String sortValue = cursor.substring(0, underscoreIndex);
                Long parsedCursorId = Long.parseLong(cursor.substring(underscoreIndex + 1));
                if (sortBy == ProductSortBy.LATEST) {
                    cursorCreatedAt = LocalDateTime.parse(sortValue);
                } else {
                    cursorPrice = Integer.parseInt(sortValue);
                }
                cursorId = parsedCursorId;
            } catch (Exception e) {
                // Invalid cursor, treat as first page
                cursorCreatedAt = null;
                cursorPrice = null;
                cursorId = null;
            }
        }

        Slice<Product> slice = productRepository.findSliceByFilters(
                condition,
                sortBy,
                cursorCreatedAt,
                cursorPrice,
                cursorId,
                PageRequest.of(0, limit)
        );
        List<Product> products = slice.getContent();
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        Map<Long, List<String>> tagsMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productTagRepository.findAllByProductIdInWithTag(productIds).stream()
                .collect(Collectors.groupingBy(
                        pt -> pt.getProduct().getId(),
                        Collectors.mapping(pt -> pt.getTag().getName(), Collectors.toList())
                ));

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
                        likeCountMap.getOrDefault(product.getId(), 0L),
                        tagsMap.getOrDefault(product.getId(), List.of())
                ))
                .toList();

        List<ProductSummaryResponse> cacheItems = userId == null
                ? items
                : items.stream()
                .map(item -> withIsLiked(item, false))
                .toList();
        
        String nextCursor = null;
        if (slice.hasNext()) {
            Product last = slice.getContent().get(slice.getContent().size() - 1);
            String nextSortValue = switch (sortBy) {
                case LATEST -> last.getCreatedAt().toString();
                case LOW_PRICE, HIGH_PRICE -> Integer.toString(last.getPrice());
            };
            nextCursor = nextSortValue + "_" + last.getId();
        }

        ProductListResponse response = new ProductListResponse(items, nextCursor);
        cacheProductList(cacheKey, new ProductListResponse(cacheItems, nextCursor));
        return response;
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

    @Transactional
    public ProductCreateResponse createProduct(Long userId, ProductCreateRequest request) {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Product product = productRepository.save(Product.builder()
                .seller(seller)
                .title(request.title())
                .description(request.description())
                .price(request.price())
                .status(ProductStatus.SELLING)
                .tradeType(request.tradeType())
                .category(category)
                .build());

        List<String> imageKeys = request.imageKeys() == null ? List.of() : request.imageKeys();
        if (!imageKeys.isEmpty()) {
            List<ProductImage> images = new ArrayList<>(imageKeys.size());
            boolean useOriginalAsImageUrl = isLocalStorage();
            for (int i = 0; i < imageKeys.size(); i++) {
                String imageKey = imageKeys.get(i);
                String originalUrl = buildOriginalUrl(imageKey);
                images.add(ProductImage.builder()
                        .product(product)
                        .originalUrl(originalUrl)
                        .imageUrl(useOriginalAsImageUrl ? originalUrl : null)
                        .thumbnailUrl(null)
                        .imageOrder(i)
                        .isThumbnail(i == 0)
                        .build());
            }
            productImageRepository.saveAll(images);
        }

        List<String> tagNames = request.tags() == null ? List.of() : request.tags();
        if (!tagNames.isEmpty()) {
            List<ProductTag> productTags = new ArrayList<>();
            for (String raw : tagNames) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String name = raw.trim();
                Tag tag = tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
                productTags.add(ProductTag.builder().product(product).tag(tag).build());
            }
            if (!productTags.isEmpty()) {
                productTagRepository.saveAll(productTags);
            }
        }

        productCacheInvalidator.invalidateProductListCache();
        return new ProductCreateResponse(product.getId());
    }

    private String buildProductsCacheKey(String cursor, ProductSearchCondition condition, ProductSortBy sortBy, int limit) {
        String filterKey = String.join("|",
                "status=" + valueOf(condition.status()),
                "category=" + valueOf(condition.categoryCode()),
                "minPrice=" + valueOf(condition.minPrice()),
                "maxPrice=" + valueOf(condition.maxPrice()),
                "tradeType=" + valueOf(condition.tradeType())
        );
        return String.join(":",
                PRODUCT_LIST_CACHE_PREFIX,
                valueOf(cursor),
                filterKey,
                valueOf(sortBy),
                Integer.toString(limit)
        );
    }

    private ProductListResponse getCachedProductList(String cacheKey) {
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue == null) {
                return null;
            }
            return objectMapper.readValue(cachedValue, ProductListResponse.class);
        } catch (Exception e) {
            log.debug("Failed to read product list cache", e);
            return null;
        }
    }

    private void cacheProductList(String cacheKey, ProductListResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, payload, PRODUCT_LIST_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Failed to write product list cache", e);
        }
    }

    private ProductListResponse applyLikedFlags(ProductListResponse cached, Long userId) {
        List<ProductSummaryResponse> items = cached.items();
        if (items == null || items.isEmpty()) {
            return cached;
        }
        List<Long> productIds = items.stream()
                .map(ProductSummaryResponse::id)
                .toList();
        Set<Long> likedProductIds = new HashSet<>(productLikeRepository.findLikedProductIds(userId, productIds));
        List<ProductSummaryResponse> updatedItems = items.stream()
                .map(item -> withIsLiked(item, likedProductIds.contains(item.id())))
                .toList();
        return new ProductListResponse(updatedItems, cached.nextCursor());
    }

    private ProductSummaryResponse withIsLiked(ProductSummaryResponse item, boolean isLiked) {
        return ProductSummaryResponse.builder()
                .id(item.id())
                .title(item.title())
                .price(item.price())
                .status(item.status())
                .tradeType(item.tradeType())
                .thumbnailUrl(item.thumbnailUrl())
                .category(item.category())
                .seller(item.seller())
                .createdAt(item.createdAt())
                .tags(item.tags())
                .isLiked(isLiked)
                .likeCount(item.likeCount())
                .build();
    }

    private String valueOf(Object value) {
        return value == null ? "null" : value.toString();
    }

    private String buildOriginalUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return imageKey;
        }
        if (imageKey.startsWith("http://") || imageKey.startsWith("https://")) {
            return imageKey;
        }
        if (storageBaseUrl == null || storageBaseUrl.isBlank()) {
            return imageKey;
        }
        String base = storageBaseUrl.endsWith("/") ? storageBaseUrl.substring(0, storageBaseUrl.length() - 1) : storageBaseUrl;
        String key = imageKey.startsWith("/") ? imageKey.substring(1) : imageKey;
        return base + "/" + key;
    }

    private boolean isLocalStorage() {
        String provider = storageProperties.provider();
        return provider != null && provider.equalsIgnoreCase("local");
    }
}
