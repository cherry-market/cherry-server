package com.cherry.server.product;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cherry.server.product.domain.Category;
import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.ProductTag;
import com.cherry.server.product.domain.Tag;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.repository.CategoryRepository;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.product.repository.ProductTagRepository;
import com.cherry.server.product.repository.TagRepository;
import com.cherry.server.user.domain.User;
import com.cherry.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductFiltersApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProductTagRepository productTagRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        productTagRepository.deleteAll();
        productRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void get_products_includes_tags_in_items() throws Exception {
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
        Product product = productRepository.save(Product.builder()
                .seller(seller)
                .title("뉴진스 포카")
                .description("desc")
                .price(1000)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(category)
                .build());
        Tag tag = tagRepository.save(Tag.builder().name("뉴진스").build());
        productTagRepository.save(ProductTag.builder().product(product).tag(tag).build());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tags", hasSize(1)))
                .andExpect(jsonPath("$.items[0].tags[0]").value("뉴진스"));
    }

    @Test
    void get_products_filters_by_category_code() throws Exception {
        User seller = userRepository.save(User.builder()
                .email("seller@example.com")
                .nickname("seller")
                .password("pw")
                .build());
        Category photo = categoryRepository.save(Category.builder()
                .code("PHOTO")
                .displayName("포토카드")
                .isActive(true)
                .sortOrder(1)
                .build());
        Category album = categoryRepository.save(Category.builder()
                .code("ALBUM")
                .displayName("앨범")
                .isActive(true)
                .sortOrder(2)
                .build());
        productRepository.save(Product.builder()
                .seller(seller)
                .title("포카")
                .description("desc")
                .price(1000)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(photo)
                .build());
        productRepository.save(Product.builder()
                .seller(seller)
                .title("앨범")
                .description("desc")
                .price(2000)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(album)
                .build());

        mockMvc.perform(get("/products").queryParam("categoryCode", "PHOTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].category.code").value("PHOTO"));
    }

    @Test
    void get_products_sorts_by_low_price() throws Exception {
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
                .title("저렴")
                .description("desc")
                .price(100)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(category)
                .build());
        productRepository.save(Product.builder()
                .seller(seller)
                .title("비쌈")
                .description("desc")
                .price(300)
                .status(ProductStatus.SELLING)
                .tradeType(TradeType.DIRECT)
                .category(category)
                .build());

        mockMvc.perform(get("/products").queryParam("sortBy", "LOW_PRICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].price").value(100))
                .andExpect(jsonPath("$.items[1].price").value(300));
    }
}

