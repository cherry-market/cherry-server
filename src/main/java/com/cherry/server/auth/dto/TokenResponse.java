package com.cherry.server.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
