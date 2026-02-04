package com.cherry.server.upload.storage;

public interface UploadUrlGenerator {
    UploadUrlResult generate(String imageKey, String contentType);
}

