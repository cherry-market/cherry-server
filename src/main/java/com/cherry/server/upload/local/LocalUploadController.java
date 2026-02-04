package com.cherry.server.upload.local;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload/images")
@ConditionalOnProperty(value = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalUploadController {

    private final LocalUploadStorage localUploadStorage;

    @PutMapping
    public ResponseEntity<Void> upload(
            @RequestParam("imageKey") String imageKey,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestHeader(value = "Content-Length", required = false) Long contentLength,
            HttpServletRequest request
    ) throws IOException {
        long length = contentLength == null ? -1 : contentLength;
        localUploadStorage.save(imageKey, contentType, request.getInputStream(), length);
        return ResponseEntity.noContent().build();
    }
}

