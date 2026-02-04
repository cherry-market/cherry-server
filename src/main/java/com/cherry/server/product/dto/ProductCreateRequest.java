package com.cherry.server.product.dto;

import com.cherry.server.product.domain.TradeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank @Size(min = 2) String title,
        @PositiveOrZero int price,
        String description,
        @NotNull Long categoryId,
        @NotNull TradeType tradeType,
        @Size(max = 10) List<String> imageKeys,
        List<String> tags
) {}
