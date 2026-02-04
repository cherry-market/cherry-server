package com.cherry.server.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cherry.server.upload.dto.UploadImagesRequest;
import com.cherry.server.upload.dto.UploadImagesResponse;
import com.cherry.server.upload.storage.UploadUrlGenerator;
import com.cherry.server.upload.storage.UploadUrlResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class UploadServiceTest {

    @Test
    void prepares_upload_items() {
        UploadUrlGenerator generator = (imageKey, contentType) -> new UploadUrlResult(
                "http://localhost:8080/api/upload/images?imageKey=" + imageKey,
                Map.of("Content-Type", contentType)
        );
        UploadService service = new UploadService(generator);

        UploadImagesRequest request = new UploadImagesRequest(List.of(
                new UploadImagesRequest.FileMeta("a.jpg", "image/jpeg", 1024),
                new UploadImagesRequest.FileMeta("b.webp", "image/webp", 2048)
        ));

        UploadImagesResponse response = service.prepare(request);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items())
                .allSatisfy(item -> {
                    assertThat(item.imageKey()).startsWith("products/original/");
                    assertThat(item.uploadUrl()).contains("imageKey=");
                    assertThat(item.requiredHeaders()).containsKey("Content-Type");
                });
    }

    @Test
    void rejects_invalid_file_extension() {
        UploadUrlGenerator generator = (imageKey, contentType) -> new UploadUrlResult(
                "http://localhost:8080/api/upload/images?imageKey=" + imageKey,
                Map.of("Content-Type", contentType)
        );
        UploadService service = new UploadService(generator);

        UploadImagesRequest request = new UploadImagesRequest(List.of(
                new UploadImagesRequest.FileMeta("a.exe", "image/jpeg", 1024)
        ));

        assertThatThrownBy(() -> service.prepare(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid file extension");
    }
}
