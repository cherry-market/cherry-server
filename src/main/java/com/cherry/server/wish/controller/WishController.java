package com.cherry.server.wish.controller;

import com.cherry.server.product.dto.ProductListResponse;
import com.cherry.server.security.UserPrincipal;
import com.cherry.server.wish.service.WishService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;

    @PostMapping("/products/{productId}/like")
    public ResponseEntity<Void> addLike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId
    ) {
        Long userId = requireUserId(principal);
        wishService.addLike(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/products/{productId}/like")
    public ResponseEntity<Void> removeLike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId
    ) {
        Long userId = requireUserId(principal);
        wishService.removeLike(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/{productId}/like-status")
    public ResponseEntity<Boolean> getLikeStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId
    ) {
        Long userId = requireUserId(principal);
        return ResponseEntity.ok(wishService.isLiked(userId, productId));
    }

    @GetMapping("/me/likes")
    public ResponseEntity<ProductListResponse> getMyLikes(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        Long userId = requireUserId(principal);
        return ResponseEntity.ok(wishService.getMyLikes(userId, cursor, limit));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return principal.id();
    }
}
