package com.cherry.server.ai;

import com.cherry.server.ai.dto.AiGenerateRequest;
import com.cherry.server.ai.dto.AiGenerateResponse;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    public AiGenerateResponse generate(AiGenerateRequest request) {
        String text = "판매글: " + request.keywords() + " (카테고리: " + request.category() + ")";
        return new AiGenerateResponse(text);
    }
}
