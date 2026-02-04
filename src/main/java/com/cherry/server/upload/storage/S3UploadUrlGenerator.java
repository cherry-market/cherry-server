package com.cherry.server.upload.storage;

import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "storage.provider", havingValue = "s3")
public class S3UploadUrlGenerator implements UploadUrlGenerator {

    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    @Override
    public UploadUrlResult generate(String imageKey, String contentType) {
        validateStorageSettings();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(imageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(storageProperties.presignExpireSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return new UploadUrlResult(
                presigned.url().toString(),
                Map.of("Content-Type", contentType)
        );
    }

    private void validateStorageSettings() {
        if (storageProperties.bucket() == null || storageProperties.bucket().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "storage.bucket is not configured");
        }
        if (storageProperties.region() == null || storageProperties.region().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "storage.region is not configured");
        }
        if (storageProperties.presignExpireSeconds() <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "storage.presign-expire-seconds is invalid");
        }
    }
}

