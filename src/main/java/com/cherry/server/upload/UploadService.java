package com.cherry.server.upload;

import com.cherry.server.upload.dto.UploadImagesRequest;
import com.cherry.server.upload.dto.UploadImagesResponse;
import com.cherry.server.upload.dto.UploadImagesResponse.Item;
import com.cherry.server.upload.storage.UploadUrlGenerator;
import com.cherry.server.upload.storage.UploadUrlResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final String ORIGINAL_PREFIX = "products/original/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final UploadUrlGenerator uploadUrlGenerator;

    public UploadImagesResponse prepare(UploadImagesRequest request) {
        if (request == null || request.files() == null || request.files().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "files is required");
        }
        List<Item> items = new ArrayList<>(request.files().size());
        for (int i = 0; i < request.files().size(); i++) {
            UploadImagesRequest.FileMeta file = request.files().get(i);
            String extension = normalizeExtension(extractExtension(file.fileName()));
            validateContentTypeMatchesExtension(file.contentType(), extension);

            String thumbFlag = (i == 0) ? "t" : "f";
            String imageKey = ORIGINAL_PREFIX + i + "_" + thumbFlag + "_" + UUID.randomUUID() + "." + extension;
            UploadUrlResult result = uploadUrlGenerator.generate(imageKey, file.contentType());

            items.add(new Item(
                    imageKey,
                    result.uploadUrl(),
                    result.requiredHeaders()
            ));
        }
        return new UploadImagesResponse(items);
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName is required");
        }
        String trimmed = fileName.trim();
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot < 0 || lastDot == trimmed.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file extension");
        }
        String ext = trimmed.substring(lastDot + 1);
        if (ext.contains("/") || ext.contains("\\") || ext.contains(":")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file extension");
        }
        return ext;
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file extension");
        }
        String normalized = extension.toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file extension");
        }
        return "jpeg".equals(normalized) ? "jpg" : normalized;
    }

    private void validateContentTypeMatchesExtension(String contentType, String extension) {
        if (contentType == null || contentType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType is required");
        }
        boolean ok = switch (contentType) {
            case "image/jpeg" -> "jpg".equals(extension) || "jpeg".equals(extension);
            case "image/png" -> "png".equals(extension);
            case "image/webp" -> "webp".equals(extension);
            default -> false;
        };
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType does not match file extension");
        }
    }
}
