package com.cherry.server.upload.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String provider,
        String bucket,
        String region,
        String baseUrl,
        long presignExpireSeconds,
        String localRoot,
        String uploadBaseUrl
) {
}
