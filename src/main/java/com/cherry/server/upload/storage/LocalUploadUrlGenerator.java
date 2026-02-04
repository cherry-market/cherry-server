package com.cherry.server.upload.storage;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalUploadUrlGenerator implements UploadUrlGenerator {

    private final StorageProperties storageProperties;

    @Override
    public UploadUrlResult generate(String imageKey, String contentType) {
        String baseUrl = storageProperties.uploadBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "storage.upload-base-url is not configured");
        }
        String uploadUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("imageKey", imageKey)
                .build()
                .toUriString();
        return new UploadUrlResult(
                uploadUrl,
                Map.of("Content-Type", contentType)
        );
    }
}

