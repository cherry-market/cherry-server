package com.cherry.server.product.controller;

import com.cherry.server.product.dto.ProductDetailResponse;
import com.cherry.server.product.dto.ProductListResponse;
import com.cherry.server.product.dto.ProductSearchCondition;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.service.ProductService;
import com.cherry.server.security.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductListResponse> getProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) TradeType tradeType,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        if (minPrice != null && minPrice < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice must be >= 0");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxPrice must be >= 0");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice must be <= maxPrice");
        }
        Long userId = principal == null ? null : principal.id();
        ProductSearchCondition condition = new ProductSearchCondition(status, categoryId, minPrice, maxPrice, tradeType);
        return ResponseEntity.ok(productService.getProducts(cursor, limit, userId, condition));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId
    ) {
        Long userId = principal == null ? null : principal.id();
        return ResponseEntity.ok(productService.getProduct(productId, userId));
    }

    @GetMapping("/trending")
    public ResponseEntity<ProductListResponse> getTrending(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal == null ? null : principal.id();
        return ResponseEntity.ok(productService.getTrending(userId));
    }
    
    // P0: Optional View Increment API (if explicit call needed)
    @PostMapping("/{productId}/views")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long productId) {
        productService.getProduct(productId, null); // Reuse logic
        return ResponseEntity.noContent().build();
    }
}
