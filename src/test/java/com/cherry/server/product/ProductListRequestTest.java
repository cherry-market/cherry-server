package com.cherry.server.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.dto.ProductListRequest;
import com.cherry.server.product.dto.ProductSortBy;
import org.junit.jupiter.api.Test;

class ProductListRequestTest {

    @Test
    void builds_request_with_sort_defaults() {
        ProductListRequest request = ProductListRequest.builder()
                .status(ProductStatus.SELLING)
                .categoryCode("photocard")
                .minPrice(1000)
                .maxPrice(5000)
                .tradeType(TradeType.DIRECT)
                .sortBy(ProductSortBy.LOW_PRICE)
                .build();

        assertThat(request.sortByOrDefault()).isEqualTo(ProductSortBy.LOW_PRICE);
        assertThat(request.categoryCode()).isEqualTo("photocard");
    }
}
