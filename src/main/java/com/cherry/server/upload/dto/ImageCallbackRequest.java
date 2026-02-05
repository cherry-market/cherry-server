package com.cherry.server.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImageCallbackRequest(
        @NotBlank String imageKey,
        @NotBlank String detailUrl,
        String thumbnailUrl,
        @NotNull Integer imageOrder,
        @NotNull Boolean isThumbnail
) {
}
