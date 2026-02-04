package com.cherry.server.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductImage;
import com.cherry.server.product.repository.ProductImageRepository;
import com.cherry.server.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "internal.token=test-internal-token",
        "storage.base-url=https://cheryi-product-images-prod.s3.ap-northeast-2.amazonaws.com"
})
class ImageCallbackEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Test
    void applies_callback_to_existing_product_image() throws Exception {
        Product product = productRepository.saveAndFlush(Product.builder()
                .title("t")
                .price(0)
                .build());

        String imageKey = "products/original/1.jpg";
        String originalUrl = "https://cheryi-product-images-prod.s3.ap-northeast-2.amazonaws.com/" + imageKey;
        productImageRepository.saveAndFlush(ProductImage.builder()
                .product(product)
                .originalUrl(originalUrl)
                .imageUrl(null)
                .thumbnailUrl(null)
                .imageOrder(0)
                .isThumbnail(true)
                .build());

        String payload = """
                {
                  "imageKey": "products/original/1.jpg",
                  "detailUrl": "https://cheryi-product-images-prod.s3.ap-northeast-2.amazonaws.com/products/detail/1.jpg",
                  "thumbnailUrl": "https://cheryi-product-images-prod.s3.ap-northeast-2.amazonaws.com/products/thumb/1_thumb.jpg",
                  "imageOrder": 1,
                  "isThumbnail": false
                }
                """;

        mockMvc.perform(post("/internal/images/complete")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNoContent());

        ProductImage updated = productImageRepository.findByOriginalUrl(originalUrl).orElseThrow();
        assertThat(updated.getImageUrl()).isEqualTo("https://cheryi-product-images-prod.s3.ap-northeast-2.amazonaws.com/products/detail/1.jpg");
        assertThat(updated.getThumbnailUrl()).isEqualTo("https://cheryi-product-images-prod.s3.ap-northeast-2.amazonaws.com/products/thumb/1_thumb.jpg");
        assertThat(updated.getImageOrder()).isEqualTo(1);
        assertThat(updated.isThumbnail()).isFalse();
    }
}

