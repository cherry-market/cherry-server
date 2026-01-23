package com.cherry.server.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GenerateGoodsImageRequest(
        @NotBlank @Size(max = 300) String promptDescription,
        @NotBlank @Pattern(regexp = "^(1:1|3:4|4:3|9:16|16:9)$") String aspectRatio
) {
}

