package com.cherry.server.dev.seed;

import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SeedDataGenerator {

    public record SeedConfig(int productCount, long seed, boolean includeImages) {}

    public record GeneratedData(
            List<UserRow> users,
            List<CategoryRow> categories,
            List<TagRow> tags,
            List<ProductRow> products,
            List<ProductImageRow> productImages,
            List<ProductTagRow> productTags
    ) {}

    public record UserRow(String email, String nickname, String passwordHash, String profileImageUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record CategoryRow(String code, String displayName, boolean isActive, int sortOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record TagRow(String name, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record ProductRow(
            Long sellerId,
            Long categoryId,
            String title,
            String description,
            int price,
            ProductStatus status,
            TradeType tradeType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ProductImageRow(Long productId, String imageUrl, int imageOrder, boolean isThumbnail, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record ProductTagRow(Long productId, Long tagId, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public GeneratedData generate(SeedConfig config) {
        Random random = new Random(config.seed());
        LocalDateTime now = LocalDateTime.now().withNano(0);

        List<CategoryRow> categories = defaultCategories(now);
        List<TagRow> tags = defaultTags(config.seed(), now);

        int sellerCount = 50;
        List<UserRow> users = new ArrayList<>(sellerCount);
        for (int i = 0; i < sellerCount; i++) {
            users.add(new UserRow(
                    "seed-" + config.seed() + "-user-" + String.format("%04d", i) + "@example.com",
                    "seed-user-" + i,
                    "{noop}pw",
                    null,
                    now,
                    now
            ));
        }

        List<ProductRow> products = new ArrayList<>(config.productCount());
        List<ProductImageRow> productImages = new ArrayList<>();
        List<ProductTagRow> productTags = new ArrayList<>();

        // IDs are assigned by the seeder; generator assumes sequential ids starting from 1.
        long firstUserId = 1L;
        long firstCategoryId = 1L;
        long firstTagId = 1L;
        long firstProductId = 1L;

        for (int i = 0; i < config.productCount(); i++) {
            long productId = firstProductId + i;
            long sellerId = firstUserId + random.nextInt(sellerCount);
            int categoryIndex = random.nextInt(categories.size());
            long categoryId = firstCategoryId + categoryIndex;

            ProductStatus status = pickStatus(random);
            TradeType tradeType = pickTradeType(random);
            int price = pickPrice(random);

            LocalDateTime createdAt = now.minusDays(random.nextInt(180)).minusMinutes(random.nextInt(24 * 60));
            LocalDateTime updatedAt = createdAt;

            products.add(new ProductRow(
                    sellerId,
                    categoryId,
                    pickTitle(random, categories.get(categoryIndex).displayName()),
                    "seed description " + i,
                    price,
                    status,
                    tradeType,
                    createdAt,
                    updatedAt
            ));

            if (config.includeImages()) {
                productImages.add(new ProductImageRow(
                        productId,
                        "https://example.invalid/seed/thumb.png?productId=" + productId,
                        0,
                        true,
                        createdAt,
                        updatedAt
                ));

                boolean isAlbum = "ALBUM".equals(categories.get(categoryIndex).code());
                if (isAlbum) {
                    for (int j = 0; j < 3; j++) {
                        productImages.add(new ProductImageRow(
                                productId,
                                "https://example.invalid/seed/album.png?productId=" + productId + "&i=" + j,
                                j,
                                false,
                                createdAt,
                                updatedAt
                        ));
                    }
                }
            }

            int tagCount = random.nextInt(6); // 0..5
            if (tagCount > 0) {
                Set<Long> chosen = new LinkedHashSet<>();
                while (chosen.size() < tagCount) {
                    long tagId = firstTagId + random.nextInt(tags.size());
                    chosen.add(tagId);
                }
                for (Long tagId : chosen) {
                    productTags.add(new ProductTagRow(productId, tagId, createdAt, updatedAt));
                }
            }
        }

        return new GeneratedData(users, categories, tags, products, productImages, productTags);
    }

    private static List<CategoryRow> defaultCategories(LocalDateTime now) {
        List<CategoryRow> categories = new ArrayList<>();
        categories.add(new CategoryRow("PHOTO", "포토카드", true, 1, now, now));
        categories.add(new CategoryRow("ALBUM", "앨범", true, 2, now, now));
        categories.add(new CategoryRow("LIGHTSTICK", "응원봉", true, 3, now, now));
        categories.add(new CategoryRow("MD", "MD", true, 4, now, now));
        categories.add(new CategoryRow("POSTER", "포스터", true, 5, now, now));
        categories.add(new CategoryRow("ETC", "기타", true, 6, now, now));
        return categories;
    }

    private static List<TagRow> defaultTags(long seed, LocalDateTime now) {
        List<TagRow> tags = new ArrayList<>(200);
        for (int i = 0; i < 200; i++) {
            tags.add(new TagRow("seed-" + seed + "-tag-" + String.format("%03d", i), now, now));
        }
        return tags;
    }

    private static ProductStatus pickStatus(Random random) {
        int r = random.nextInt(100);
        if (r < 70) return ProductStatus.SELLING;
        if (r < 90) return ProductStatus.RESERVED;
        return ProductStatus.SOLD;
    }

    private static TradeType pickTradeType(Random random) {
        int r = random.nextInt(100);
        if (r < 40) return TradeType.DIRECT;
        if (r < 80) return TradeType.DELIVERY;
        return TradeType.BOTH;
    }

    private static int pickPrice(Random random) {
        int bucket = random.nextInt(100);
        if (bucket < 60) return 1000 + random.nextInt(49000); // 1k..50k
        if (bucket < 90) return 50000 + random.nextInt(150000); // 50k..200k
        return 200000 + random.nextInt(300000); // 200k..500k
    }

    private static String pickTitle(Random random, String categoryName) {
        String[] adjectives = {"새상품", "미개봉", "급처", "레어", "한정"};
        return adjectives[random.nextInt(adjectives.length)] + " " + categoryName + " " + (1000 + random.nextInt(9000));
    }
}
