package com.cherry.server.upload.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cherry.server.upload.storage.StorageProperties;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

class LocalUploadStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void saves_file_under_root() throws Exception {
        StorageProperties properties = new StorageProperties(
                "local",
                null,
                null,
                "http://localhost:8080/uploads",
                0,
                tempDir.toString(),
                "http://localhost:8080/api/upload/images"
        );
        LocalUploadStorage storage = new LocalUploadStorage(properties);

        byte[] payload = "hello".getBytes();
        storage.save("products/original/1.jpg", "image/jpeg", new ByteArrayInputStream(payload), payload.length);

        Path saved = tempDir.resolve("products/original/1.jpg");
        assertThat(Files.exists(saved)).isTrue();
        assertThat(Files.readAllBytes(saved)).isEqualTo(payload);
    }

    @Test
    void rejects_invalid_key() {
        StorageProperties properties = new StorageProperties(
                "local",
                null,
                null,
                "http://localhost:8080/uploads",
                0,
                tempDir.toString(),
                "http://localhost:8080/api/upload/images"
        );
        LocalUploadStorage storage = new LocalUploadStorage(properties);

        assertThatThrownBy(() -> storage.save("../secret.txt", "image/jpeg", new ByteArrayInputStream(new byte[1]), 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("imageKey");
    }
}

