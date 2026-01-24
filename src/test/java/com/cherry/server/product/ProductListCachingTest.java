package com.cherry.server.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cherry.server.product.domain.Category;
import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.dto.ProductSearchCondition;
import com.cherry.server.product.dto.ProductSortBy;
import com.cherry.server.product.repository.CategoryRepository;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.user.domain.User;
import com.cherry.server.user.repository.UserRepository;
import com.cherry.server.wish.domain.ProductLike;
import com.cherry.server.wish.repository.ProductLikeRepository;
import com.cherry.server.wish.service.WishService;
import com.cherry.server.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("test")
class ProductListCachingTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private WishService wishService;

    @SpyBean
    private ProductRepository productRepository;

    @SpyBean
    private ProductLikeRepository productLikeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    private Map<String, String> cacheStore;

    @BeforeEach
    void setUp() {
        productLikeRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        cacheStore = new ConcurrentHashMap<>();
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> cacheStore.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            cacheStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void get_products_uses_cache_on_repeat_calls() {
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
        productRepository.save(Product.builder()
                .seller(seller)
                .title("테스트 상품")
                .description("desc")
                .price(1000)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(category)
                .build());

        clearInvocations(productRepository);

        ProductSearchCondition condition = new ProductSearchCondition(
                null, null, null, null, null, ProductSortBy.LATEST
        );

        productService.getProducts(null, 20, null, condition, ProductSortBy.LATEST);
        productService.getProducts(null, 20, null, condition, ProductSortBy.LATEST);

        verify(productRepository, times(1)).findSliceByFilters(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void get_my_likes_uses_cache_on_repeat_calls() {
        User user = userRepository.save(User.builder()
                .email("user@example.com")
                .nickname("user")
                .password("pw")
                .build());
        Category category = categoryRepository.save(Category.builder()
                .code("PHOTO")
                .displayName("포토카드")
                .isActive(true)
                .sortOrder(1)
                .build());
        Product product = productRepository.save(Product.builder()
                .seller(user)
                .title("테스트 상품")
                .description("desc")
                .price(1000)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(category)
                .build());
        productLikeRepository.save(ProductLike.create(user, product));

        clearInvocations(productLikeRepository);

        wishService.getMyLikes(user.getId(), null, 20);
        wishService.getMyLikes(user.getId(), null, 20);

        verify(productLikeRepository, times(1)).findAllByUserIdWithProductCursor(
                any(),
                any(),
                any(),
                any()
        );
    }
}
