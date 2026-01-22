package com.cherry.server.product.controller;

import com.cherry.server.product.dto.ProductDetailResponse;
import com.cherry.server.product.dto.ProductListResponse;
import com.cherry.server.product.service.ProductService;
import com.cherry.server.security.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        Long userId = principal == null ? null : principal.id();
        return ResponseEntity.ok(productService.getProducts(cursor, limit, userId));
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
