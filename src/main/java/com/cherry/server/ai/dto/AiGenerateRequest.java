package com.cherry.server.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiGenerateRequest(
        @NotBlank String keywords,
        @NotBlank String category
) {
}
