package com.cherry.server.upload.storage;

import java.util.Map;

public record UploadUrlResult(
        String uploadUrl,
        Map<String, String> requiredHeaders
) {
}

