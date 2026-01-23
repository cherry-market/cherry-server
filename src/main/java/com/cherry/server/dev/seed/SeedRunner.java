package com.cherry.server.dev.seed;

import com.cherry.server.dev.seed.SeedDataGenerator.SeedConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@Profile("local")
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true")
public class SeedRunner implements CommandLineRunner {

    private final SeedJdbcSeeder seeder;
    private final SeedDataGenerator generator;
    private final int count;
    private final long seed;
    private final boolean includeImages;
    private final boolean truncate;
    private final String confirm;

    public SeedRunner(
            SeedJdbcSeeder seeder,
            SeedDataGenerator generator,
            @Value("${seed.count:10000}") int count,
            @Value("${seed.seed:42}") long seed,
            @Value("${seed.include-images:false}") boolean includeImages,
            @Value("${seed.truncate:false}") boolean truncate,
            @Value("${seed.confirm:}") String confirm
    ) {
        this.seeder = seeder;
        this.generator = generator;
        this.count = count;
        this.seed = seed;
        this.includeImages = includeImages;
        this.truncate = truncate;
        this.confirm = confirm;
    }

    @Override
    public void run(String... args) {
        run();
    }

    void run() {
        if (truncate && !"YES".equals(confirm)) {
            throw new IllegalArgumentException("Refusing to truncate without seed.confirm=YES");
        }
        SeedDataGenerator.GeneratedData data = generator.generate(new SeedConfig(count, seed, includeImages));
        seeder.seed(data, new SeedJdbcSeeder.SeedRunOptions(truncate, confirm));
        System.out.println("[seed] done: products=" + count + " seed=" + seed + " truncate=" + truncate);
    }
}
