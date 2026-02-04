package com.cherry.server.upload.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LocalUploadUrlGeneratorTest {

    @Test
    void builds_local_upload_url_with_image_key() {
        StorageProperties properties = new StorageProperties(
                "local",
                null,
                null,
                "http://localhost:8080/uploads",
                0,
                "./uploads",
                "http://localhost:8080/api/upload/images"
        );
        LocalUploadUrlGenerator generator = new LocalUploadUrlGenerator(properties);

        UploadUrlResult result = generator.generate("products/original/1.jpg", "image/jpeg");

        assertThat(result.uploadUrl())
                .isEqualTo("http://localhost:8080/api/upload/images?imageKey=products%2Foriginal%2F1.jpg");
        assertThat(result.requiredHeaders()).containsEntry("Content-Type", "image/jpeg");
    }

    @Test
    void rejects_missing_upload_base_url() {
        StorageProperties properties = new StorageProperties(
                "local",
                null,
                null,
                "http://localhost:8080/uploads",
                0,
                "./uploads",
                null
        );
        LocalUploadUrlGenerator generator = new LocalUploadUrlGenerator(properties);

        assertThatThrownBy(() -> generator.generate("products/original/1.jpg", "image/jpeg"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("storage.upload-base-url");
    }
}

