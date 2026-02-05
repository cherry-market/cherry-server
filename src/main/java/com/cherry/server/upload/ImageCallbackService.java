package com.cherry.server.upload;

import com.cherry.server.product.cache.ProductCacheInvalidator;
import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductImage;
import com.cherry.server.product.repository.ProductImageRepository;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.upload.dto.ImageCallbackRequest;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCallbackService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ProductCacheInvalidator productCacheInvalidator;
    private final EntityManager entityManager;

    @Value("${storage.base-url:}")
    private String storageBaseUrl;

    @Transactional
    public void apply(ImageCallbackRequest request) {
        String originalUrl = buildOriginalUrl(request.imageKey());
        ProductImage image = productImageRepository.findByOriginalUrl(originalUrl)
                .orElseThrow(() -> new IllegalArgumentException("Image not found for key: " + request.imageKey()));

        // Idempotency: skip if already processed
        if (image.isProcessed()) {
            log.info("Image already processed, skipping: imageKey={}", request.imageKey());
            return;
        }

        image.updateProcessedImage(
                request.detailUrl(),
                request.thumbnailUrl()
        );

        log.info("Image processed: imageKey={}, imageOrder={}, isThumbnail={}",
                request.imageKey(), request.imageOrder(), request.isThumbnail());

        // Flush to ensure count query sees the updated row
        entityManager.flush();

        // Check if all images for this product are processed
        Product product = image.getProduct();
        long totalImages = productImageRepository.countByProductId(product.getId());
        long processedImages = productImageRepository.countByProductIdAndImageUrlIsNotNull(product.getId());

        log.info("Image processing progress: productId={}, processed={}/{}",
                product.getId(), processedImages, totalImages);

        if (processedImages >= totalImages) {
            product.activate();
            productCacheInvalidator.invalidateProductListCache();
            log.info("All images processed, product activated: productId={}", product.getId());
        }
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
