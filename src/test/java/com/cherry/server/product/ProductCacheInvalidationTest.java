package com.cherry.server.product;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.cherry.server.product.cache.ProductCacheInvalidator;
import com.cherry.server.product.domain.Category;
import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.repository.CategoryRepository;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.user.domain.User;
import com.cherry.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@ActiveProfiles("test")
class ProductCacheInvalidationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private ProductCacheInvalidator productCacheInvalidator;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        clearInvocations(productCacheInvalidator);
    }

    @Test
    void create_product_invalidates_cache() {
        Product product = productRepository.save(buildProduct("상품"));

        productRepository.flush();

        verify(productCacheInvalidator, times(1)).invalidateProductListCache();
    }

    @Test
    void update_product_invalidates_cache() {
        Product product = productRepository.save(buildProduct("상품"));
        clearInvocations(productCacheInvalidator);

        ReflectionTestUtils.setField(product, "title", "상품-수정");
        productRepository.save(product);
        productRepository.flush();

        verify(productCacheInvalidator, times(1)).invalidateProductListCache();
    }

    @Test
    void delete_product_invalidates_cache() {
        Product product = productRepository.save(buildProduct("상품"));
        clearInvocations(productCacheInvalidator);

        productRepository.delete(product);
        productRepository.flush();

        verify(productCacheInvalidator, times(1)).invalidateProductListCache();
    }

    private Product buildProduct(String title) {
        User seller = userRepository.save(User.builder()
                .email("seller@example.com")
                .nickname("seller")
                .password("pw")
                .build());
        Category category = categoryRepository.save(Category.builder()
                .code("PHOTO")
                .displayName("포토카드")
                .isActive(true)
                .sortOrder(1)
                .build());
        return Product.builder()
                .seller(seller)
                .title(title)
                .description("desc")
                .price(1000)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(category)
                .build();
    }
}
