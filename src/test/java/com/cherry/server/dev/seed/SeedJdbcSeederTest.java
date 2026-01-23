package com.cherry.server.dev.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.cherry.server.dev.seed.SeedDataGenerator.SeedConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SeedJdbcSeederTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SeedJdbcSeeder seeder;

    @Test
    void seed_inserts_rows_into_core_tables() {
        SeedDataGenerator.GeneratedData data = new SeedDataGenerator().generate(new SeedConfig(50, 123L, true));

        seeder.seed(data, new SeedJdbcSeeder.SeedRunOptions(true, "YES"));

        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer categoryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);
        Integer tagCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tags", Integer.class);
        Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        Integer imageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_images", Integer.class);

        assertThat(userCount).isEqualTo(50);
        assertThat(categoryCount).isGreaterThan(0);
        assertThat(tagCount).isEqualTo(200);
        assertThat(productCount).isEqualTo(50);
        assertThat(imageCount).isGreaterThan(0);
    }
}

