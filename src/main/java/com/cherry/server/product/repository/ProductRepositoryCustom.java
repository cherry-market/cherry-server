package com.cherry.server.product.repository;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.dto.ProductSearchCondition;
import com.cherry.server.product.dto.ProductSortBy;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ProductRepositoryCustom {
    Slice<Product> findSliceByFilters(
            ProductSearchCondition condition,
            ProductSortBy sortBy,
            LocalDateTime cursorCreatedAt,
            Integer cursorPrice,
            Long cursorId,
            Pageable pageable
    );
}

