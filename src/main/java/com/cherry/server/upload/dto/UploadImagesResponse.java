package com.cherry.server.upload.dto;

import java.util.List;
import java.util.Map;

public record UploadImagesResponse(
        List<Item> items
) {
    public record Item(
            String imageKey,
            String uploadUrl,
            Map<String, String> requiredHeaders
    ) {}
}
