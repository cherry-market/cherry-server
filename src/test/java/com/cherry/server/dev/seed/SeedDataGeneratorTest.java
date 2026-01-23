package com.cherry.server.dev.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.cherry.server.dev.seed.SeedDataGenerator.SeedConfig;
import org.junit.jupiter.api.Test;

class SeedDataGeneratorTest {

    @Test
    void generate_is_deterministic_for_same_seed() {
        SeedConfig config = new SeedConfig(100, 42L, false);

        SeedDataGenerator generator = new SeedDataGenerator();
        SeedDataGenerator.GeneratedData first = generator.generate(config);
        SeedDataGenerator.GeneratedData second = generator.generate(config);

        assertThat(first.users()).hasSize(50);
        assertThat(first.categories()).isNotEmpty();
        assertThat(first.tags()).hasSize(200);
        assertThat(first.products()).hasSize(100);
        assertThat(first.productTags()).isNotEmpty();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void generate_assigns_required_fields() {
        SeedConfig config = new SeedConfig(50, 7L, true);

        SeedDataGenerator.GeneratedData data = new SeedDataGenerator().generate(config);

        assertThat(data.products()).allSatisfy(p -> {
            assertThat(p.title()).isNotBlank();
            assertThat(p.price()).isGreaterThan(0);
            assertThat(p.sellerId()).isNotNull();
            assertThat(p.status()).isNotNull();
            assertThat(p.tradeType()).isNotNull();
            assertThat(p.createdAt()).isNotNull();
        });

        assertThat(data.productImages()).allSatisfy(img -> {
            assertThat(img.productId()).isNotNull();
            assertThat(img.imageUrl()).isNotBlank();
            assertThat(img.imageOrder()).isGreaterThanOrEqualTo(0);
        });
    }
}

