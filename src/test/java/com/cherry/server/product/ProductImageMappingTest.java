package com.cherry.server.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.cherry.server.product.domain.Category;
import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductImage;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.dto.ProductDetailResponse;
import com.cherry.server.product.dto.ProductSummaryResponse;
import com.cherry.server.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductImageMappingTest {

    @Test
    void detail_includes_thumbnail_images_in_order() {
        User seller = User.builder()
                .email("seller@example.com")
                .nickname("seller")
                .password("pw")
                .build();
        Category category = Category.builder()
                .code("PHOTO")
                .displayName("포토카드")
                .isActive(true)
                .sortOrder(1)
                .build();
        Product product = Product.builder()
                .seller(seller)
                .title("title")
                .description("desc")
                .price(1000)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(category)
                .build();

        ProductImage first = ProductImage.builder()
                .product(product)
                .imageUrl("https://cdn/detail-0.jpg")
                .imageOrder(0)
                .isThumbnail(true)
                .build();
        ProductImage second = ProductImage.builder()
                .product(product)
                .imageUrl("https://cdn/detail-1.jpg")
                .imageOrder(1)
                .isThumbnail(false)
                .build();
        product.getImages().addAll(List.of(first, second));

        ProductDetailResponse response = ProductDetailResponse.from(product);

        assertThat(response.imageUrls()).containsExactly(
                "https://cdn/detail-0.jpg",
                "https://cdn/detail-1.jpg"
        );
    }

    @Test
    void summary_prefers_thumbnail_url_when_present() {
        User seller = User.builder()
                .email("seller@example.com")
                .nickname("seller")
                .password("pw")
                .build();
        Category category = Category.builder()
                .code("PHOTO")
                .displayName("포토카드")
                .isActive(true)
                .sortOrder(1)
                .build();
        Product product = Product.builder()
                .seller(seller)
                .title("title")
                .description("desc")
                .price(1000)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(category)
                .build();

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl("https://cdn/detail.jpg")
                .thumbnailUrl("https://cdn/thumb.jpg")
                .imageOrder(0)
                .isThumbnail(true)
                .build();
        product.getImages().add(image);

        ProductSummaryResponse response = ProductSummaryResponse.from(product, false, 0L, List.of());

        assertThat(response.thumbnailUrl()).isEqualTo("https://cdn/thumb.jpg");
    }
}
