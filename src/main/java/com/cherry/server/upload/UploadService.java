package com.cherry.server.upload;

import com.cherry.server.upload.dto.UploadImagesRequest;
import com.cherry.server.upload.dto.UploadImagesResponse;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class UploadService {

    public UploadImagesResponse prepare(UploadImagesRequest request) {
        return new UploadImagesResponse(Collections.emptyList());
    }
}
