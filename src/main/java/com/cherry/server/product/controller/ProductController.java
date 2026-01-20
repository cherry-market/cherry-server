package com.cherry.server.product.controller;

import com.cherry.server.product.dto.ProductDetailResponse;
import com.cherry.server.product.dto.ProductListResponse;
import com.cherry.server.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductListResponse> getProducts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(productService.getProducts(cursor, limit));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    @GetMapping("/trending")
    public ResponseEntity<ProductListResponse> getTrending() {
        return ResponseEntity.ok(productService.getTrending());
    }
    
    // P0: Optional View Increment API (if explicit call needed)
    @PostMapping("/{productId}/views")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long productId) {
        productService.getProduct(productId); // Reuse logic
        return ResponseEntity.noContent().build();
    }
}
