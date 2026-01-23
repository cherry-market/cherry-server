package com.cherry.server.product.dto;

import com.cherry.server.product.domain.Category;

public record CategoryResponse(
        Long id,
        String code,
        String displayName
) {
    public static CategoryResponse from(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(category.getId(), category.getCode(), category.getDisplayName());
    }
}

