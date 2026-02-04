package com.cherry.server.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.cherry.server.product.domain.Category;
import com.cherry.server.product.domain.ProductImage;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.dto.ProductCreateRequest;
import com.cherry.server.product.dto.ProductCreateResponse;
import com.cherry.server.product.repository.CategoryRepository;
import com.cherry.server.product.repository.ProductImageRepository;
import com.cherry.server.product.service.ProductService;
import com.cherry.server.product.cache.ProductCacheInvalidator;
import com.cherry.server.user.domain.User;
import com.cherry.server.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "storage.provider=local",
        "storage.base-url=http://localhost:8080/uploads"
})
class ProductCreateLocalImageFallbackTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @MockBean
    private ProductCacheInvalidator productCacheInvalidator;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void stores_original_url_as_image_url_when_local() {
        User user = userRepository.save(User.builder()
                .email("local@test.com")
                .nickname("local")
                .password("pw")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .code("LOCAL")
                .displayName("Local")
                .isActive(true)
                .sortOrder(1)
                .build());

        String imageKey = "products/original/" + UUID.randomUUID() + ".jpg";
        ProductCreateRequest request = new ProductCreateRequest(
                "title",
                1000,
                "desc",
                category.getId(),
                TradeType.DIRECT,
                List.of(imageKey),
                List.of()
        );

        ProductCreateResponse response = productService.createProduct(user.getId(), request);
        assertThat(response.productId()).isNotNull();

        String expectedOriginalUrl = "http://localhost:8080/uploads/" + imageKey;
        ProductImage image = productImageRepository.findByOriginalUrl(expectedOriginalUrl).orElseThrow();
        assertThat(image.getImageUrl()).isEqualTo(expectedOriginalUrl);
    }
}

