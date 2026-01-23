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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishService {

    private final ProductLikeRepository productLikeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public void addLike(Long userId, Long productId) {
        User user = getUser(userId);
        Product product = getProduct(productId);

        if (productLikeRepository.existsByUserAndProduct(user, product)) {
            return;
        }

        try {
            productLikeRepository.save(ProductLike.create(user, product));
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    public void removeLike(Long userId, Long productId) {
        User user = getUser(userId);
        Product product = getProduct(productId);
        productLikeRepository.deleteByUserAndProduct(user, product);
    }

    @Transactional(readOnly = true)
    public ProductListResponse getMyLikes(Long userId, String cursor, int limit) {
        getUser(userId);
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

        return new ProductListResponse(items, nextCursor);
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
}
