package com.cherry.server.upload;

import com.cherry.server.product.domain.ProductImage;
import com.cherry.server.product.repository.ProductImageRepository;
import com.cherry.server.upload.dto.ImageCallbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageCallbackService {

    private final ProductImageRepository productImageRepository;

    @Value("${storage.base-url:}")
    private String storageBaseUrl;

    @Transactional
    public void apply(ImageCallbackRequest request) {
        String originalUrl = buildOriginalUrl(request.imageKey());
        ProductImage image = productImageRepository.findByOriginalUrl(originalUrl)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        image.updateProcessedImage(
                request.detailUrl(),
                request.thumbnailUrl(),
                request.imageOrder(),
                request.isThumbnail()
        );
    }

    private String buildOriginalUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return imageKey;
        }
        if (imageKey.startsWith("http://") || imageKey.startsWith("https://")) {
            return imageKey;
        }
        if (storageBaseUrl == null || storageBaseUrl.isBlank()) {
            return imageKey;
        }
        String base = storageBaseUrl.endsWith("/") ? storageBaseUrl.substring(0, storageBaseUrl.length() - 1) : storageBaseUrl;
        String key = imageKey.startsWith("/") ? imageKey.substring(1) : imageKey;
        return base + "/" + key;
    }
}
