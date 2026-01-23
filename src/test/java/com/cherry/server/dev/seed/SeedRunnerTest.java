package com.cherry.server.dev.seed;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.cherry.server.dev.seed.SeedDataGenerator.SeedConfig;
import org.junit.jupiter.api.Test;

class SeedRunnerTest {

    @Test
    void run_refuses_truncate_without_confirm_yes() {
        SeedJdbcSeeder seeder = mock(SeedJdbcSeeder.class);
        SeedRunner runner = new SeedRunner(
                seeder,
                new SeedDataGenerator(),
                10,
                1L,
                true,
                true,
                ""
        );

        assertThatThrownBy(() -> runner.run())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seed.confirm=YES");
    }

    @Test
    void run_calls_seeder_with_generated_data() throws Exception {
        SeedJdbcSeeder seeder = mock(SeedJdbcSeeder.class);
        SeedRunner runner = new SeedRunner(
                seeder,
                new SeedDataGenerator(),
                10,
                1L,
                true,
                false,
                ""
        );

        runner.run();

        verify(seeder, times(1)).seed(any(SeedDataGenerator.GeneratedData.class), eq(new SeedJdbcSeeder.SeedRunOptions(false, "")));
    }
}

