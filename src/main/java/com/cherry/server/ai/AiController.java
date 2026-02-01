package com.cherry.server.ai;

import com.cherry.server.ai.dto.AiGenerateRequest;
import com.cherry.server.ai.dto.AiGenerateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate-description")
    public AiGenerateResponse generate(@Valid @RequestBody AiGenerateRequest request) {
        return aiService.generate(request);
    }
}
