package com.cherry.server.upload;

import com.cherry.server.upload.dto.UploadImagesRequest;
import com.cherry.server.upload.dto.UploadImagesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/images")
    public UploadImagesResponse prepare(@Valid @RequestBody UploadImagesRequest request) {
        return uploadService.prepare(request);
    }
}
