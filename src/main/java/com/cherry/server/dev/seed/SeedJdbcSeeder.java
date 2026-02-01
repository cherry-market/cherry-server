package com.cherry.server.dev.seed;

import com.cherry.server.dev.seed.SeedDataGenerator.GeneratedData;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SeedJdbcSeeder {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void seed(GeneratedData data, SeedRunOptions options) {
        if (options.truncate()) {
            truncateAll(options.confirm());
        }

        long userIdStart = nextId("users");
        long categoryIdStart = nextId("categories");
        long tagIdStart = nextId("tags");
        long productIdStart = nextId("products");
        long productImageIdStart = nextId("product_images");
        long productTagIdStart = nextId("product_tags");

        insertUsers(data.users(), userIdStart);
        insertCategories(data.categories(), categoryIdStart);
        insertTags(data.tags(), tagIdStart);
        insertProducts(data.products(), productIdStart, userIdStart, categoryIdStart);
        insertProductImages(data.productImages(), productImageIdStart, productIdStart);
        insertProductTags(data.productTags(), productTagIdStart, productIdStart, tagIdStart);
    }

    public record SeedRunOptions(boolean truncate, String confirm) {}

    private void truncateAll(String confirm) {
        if (!"YES".equals(confirm)) {
            throw new IllegalArgumentException("Refusing to truncate without seed.confirm=YES");
        }
        jdbcTemplate.update("DELETE FROM product_images");
        jdbcTemplate.update("DELETE FROM product_likes");
        jdbcTemplate.update("DELETE FROM product_tags");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM tags");
        jdbcTemplate.update("DELETE FROM categories");
        jdbcTemplate.update("DELETE FROM users");
    }

    private long nextId(String table) {
        Long max = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + table, Long.class);
        return (max == null ? 0 : max) + 1;
    }

    private void insertUsers(List<SeedDataGenerator.UserRow> users, long idStart) {
        for (int i = 0; i < users.size(); i++) {
            SeedDataGenerator.UserRow row = users.get(i);
            long id = idStart + i;
            jdbcTemplate.update(
                    "INSERT INTO users (id, email, nickname, password, profile_image_url, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    id,
                    row.email(),
                    row.nickname(),
                    row.passwordHash(),
                    row.profileImageUrl(),
                    Timestamp.valueOf(row.createdAt()),
                    Timestamp.valueOf(row.updatedAt())
            );
        }
    }

    private void insertCategories(List<SeedDataGenerator.CategoryRow> categories, long idStart) {
        for (int i = 0; i < categories.size(); i++) {
            SeedDataGenerator.CategoryRow row = categories.get(i);
            long id = idStart + i;
            jdbcTemplate.update(
                    "INSERT INTO categories (id, code, display_name, is_active, sort_order, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    id,
                    row.code(),
                    row.displayName(),
                    row.isActive(),
                    row.sortOrder(),
                    Timestamp.valueOf(row.createdAt()),
                    Timestamp.valueOf(row.updatedAt())
            );
        }
    }

    private void insertTags(List<SeedDataGenerator.TagRow> tags, long idStart) {
        for (int i = 0; i < tags.size(); i++) {
            SeedDataGenerator.TagRow row = tags.get(i);
            long id = idStart + i;
            jdbcTemplate.update(
                    "INSERT INTO tags (id, name, created_at, updated_at) VALUES (?, ?, ?, ?)",
                    id,
                    row.name(),
                    Timestamp.valueOf(row.createdAt()),
                    Timestamp.valueOf(row.updatedAt())
            );
        }
    }

    private void insertProducts(
            List<SeedDataGenerator.ProductRow> products,
            long productIdStart,
            long userIdStart,
            long categoryIdStart
    ) {
        for (int i = 0; i < products.size(); i++) {
            SeedDataGenerator.ProductRow row = products.get(i);
            long id = productIdStart + i;
            jdbcTemplate.update(
                    "INSERT INTO products (id, seller_user_id, category_id, title, description, price, status, trade_type, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    id,
                    userIdStart + (row.sellerId() - 1),
                    categoryIdStart + (row.categoryId() - 1),
                    row.title(),
                    row.description(),
                    row.price(),
                    row.status().name(),
                    row.tradeType().name(),
                    Timestamp.valueOf(row.createdAt()),
                    Timestamp.valueOf(row.updatedAt())
            );
        }
    }

    private void insertProductImages(
            List<SeedDataGenerator.ProductImageRow> images,
            long productImageIdStart,
            long productIdStart
    ) {
        for (int i = 0; i < images.size(); i++) {
            SeedDataGenerator.ProductImageRow row = images.get(i);
            long id = productImageIdStart + i;
            jdbcTemplate.update(
                    "INSERT INTO product_images (id, product_id, image_url, original_url, thumbnail_url, image_order, is_thumbnail, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    id,
                    productIdStart + (row.productId() - 1),
                    row.imageUrl(),
                    row.originalUrl(),
                    row.thumbnailUrl(),
                    row.imageOrder(),
                    row.isThumbnail(),
                    Timestamp.valueOf(row.createdAt()),
                    Timestamp.valueOf(row.updatedAt())
            );
        }
    }

    private void insertProductTags(
            List<SeedDataGenerator.ProductTagRow> productTags,
            long productTagIdStart,
            long productIdStart,
            long tagIdStart
    ) {
        for (int i = 0; i < productTags.size(); i++) {
            SeedDataGenerator.ProductTagRow row = productTags.get(i);
            long id = productTagIdStart + i;
            jdbcTemplate.update(
                    "INSERT INTO product_tags (id, product_id, tag_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                    id,
                    productIdStart + (row.productId() - 1),
                    tagIdStart + (row.tagId() - 1),
                    Timestamp.valueOf(row.createdAt()),
                    Timestamp.valueOf(row.updatedAt())
            );
        }
    }
}

