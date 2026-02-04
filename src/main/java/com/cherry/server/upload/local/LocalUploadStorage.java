package com.cherry.server.upload.local;

import com.cherry.server.upload.storage.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalUploadStorage {

    private static final String ORIGINAL_PREFIX = "products/original/";
    private static final long MAX_FILE_SIZE_BYTES = 10_485_760L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final StorageProperties storageProperties;

    public void save(String imageKey, String contentType, InputStream inputStream, long contentLength) {
        if (imageKey == null || imageKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageKey is required");
        }
        if (!imageKey.startsWith(ORIGINAL_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageKey must start with products/original/");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType is required");
        }
        if (contentLength > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large");
        }

        String extension = normalizeExtension(extractExtension(imageKey));
        validateContentTypeMatchesExtension(contentType, extension);

        Path root = resolveRootPath();
        Path target = root.resolve(imageKey).normalize();
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid imageKey");
        }

        try {
            Files.createDirectories(target.getParent());
            writeWithSizeLimit(target, inputStream, contentLength);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
    }

    private Path resolveRootPath() {
        String localRoot = storageProperties.localRoot();
        if (localRoot == null || localRoot.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "storage.local-root is not configured");
        }
        return Paths.get(localRoot).toAbsolutePath().normalize();
    }

    private void writeWithSizeLimit(Path target, InputStream inputStream, long contentLength) throws IOException {
        if (contentLength > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large");
        }

        long total = 0;
        byte[] buffer = new byte[8192];
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FILE_SIZE_BYTES) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large");
                }
                outputStream.write(buffer, 0, read);
            }
        } catch (ResponseStatusException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    private String extractExtension(String fileName) {
        String trimmed = fileName.trim();
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot < 0 || lastDot == trimmed.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file extension");
        }
        return trimmed.substring(lastDot + 1);
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file extension");
        }
        String normalized = extension.toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file extension");
        }
        return normalized;
    }

    private void validateContentTypeMatchesExtension(String contentType, String extension) {
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
