package com.cherry.server.ai;

import com.cherry.server.ai.dto.AiGenerateRequest;
import com.cherry.server.ai.dto.AiGenerateResponse;
import com.cherry.server.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate-description")
    public AiGenerateResponse generate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiGenerateRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return aiService.generate(principal.id(), request);
    }
}
