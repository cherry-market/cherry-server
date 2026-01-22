package com.cherry.server.security;

public record UserPrincipal(
        Long id,
        String email
) {
}
