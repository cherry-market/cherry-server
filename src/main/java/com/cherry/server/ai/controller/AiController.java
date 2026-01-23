package com.cherry.server.ai.controller;

import com.cherry.server.ai.dto.GenerateGoodsImageRequest;
import com.cherry.server.ai.dto.GenerateGoodsImageResponse;
import com.cherry.server.ai.service.GeminiImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final GeminiImageService geminiImageService;

    @PostMapping("/goods-image")
    public ResponseEntity<GenerateGoodsImageResponse> generateGoodsImage(
            @Valid @RequestBody GenerateGoodsImageRequest request
    ) {
        return geminiImageService.generateGoodsImage(request.promptDescription(), request.aspectRatio())
                .map(url -> ResponseEntity.ok(new GenerateGoodsImageResponse(url)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}

