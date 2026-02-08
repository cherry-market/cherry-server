package com.cherry.server.ai;

import com.cherry.server.ai.dto.AiGenerateRequest;
import com.cherry.server.ai.dto.AiGenerateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiService {

    private static final int DAILY_LIMIT = 5;
    private static final long COOLDOWN_SECONDS = 10;
    private static final String DAILY_KEY_PREFIX = "ai:limit:daily:";
    private static final String COOLDOWN_KEY_PREFIX = "ai:limit:cooldown:";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final String apiKey;

    public AiService(
            @Value("${gemini.api-key:}") String apiKey,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate
    ) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public AiGenerateResponse generate(Long userId, AiGenerateRequest request) {
        checkRateLimit(userId);

        AiGenerateResponse response;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key not configured, returning fallback");
            response = callFallback(request);
        } else {
            response = callGemini(request);
        }

        // 성공 시 카운터 증가 + 쿨다운 설정
        incrementUsage(userId);

        int remaining = getRemainingCount(userId);
        return new AiGenerateResponse(response.generatedDescription(), remaining);
    }

    private void checkRateLimit(Long userId) {
        // 쿨다운 체크
        String cooldownKey = COOLDOWN_KEY_PREFIX + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "잠시 후 다시 시도해주세요 (10초 쿨다운)");
        }

        // 일일 횟수 체크
        String dailyKey = DAILY_KEY_PREFIX + userId;
        String countStr = redisTemplate.opsForValue().get(dailyKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        if (count >= DAILY_LIMIT) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "오늘 사용 횟수를 초과했습니다 (일일 " + DAILY_LIMIT + "회)");
        }
    }

    private void incrementUsage(Long userId) {
        String dailyKey = DAILY_KEY_PREFIX + userId;
        Long newCount = redisTemplate.opsForValue().increment(dailyKey);
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(dailyKey, 24, TimeUnit.HOURS);
        }

        String cooldownKey = COOLDOWN_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);
    }

    public int getRemainingCount(Long userId) {
        String dailyKey = DAILY_KEY_PREFIX + userId;
        String countStr = redisTemplate.opsForValue().get(dailyKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        return Math.max(0, DAILY_LIMIT - count);
    }

    private AiGenerateResponse callGemini(AiGenerateRequest request) {
        try {
            String prompt = buildPrompt(request);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.8,
                            "maxOutputTokens", 500
                    )
            );

            String responseBody = restClient.post()
                    .uri("/v1beta/models/gemini-2.0-flash:generateContent?key={key}", apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("");

            if (text.isBlank()) {
                log.warn("Empty response from Gemini API");
                return callFallback(request);
            }

            return new AiGenerateResponse(text.trim(), 0);
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return callFallback(request);
        }
    }

    private String buildPrompt(AiGenerateRequest request) {
        String personality = request.personality() != null ? request.personality() : "친근함";
        String tone = request.tone() != null ? request.tone() : "POLITE";

        String toneInstruction = switch (tone) {
            case "SHORT" -> """
                    - 음슴체로 작성 (예: '상태 좋음', '미개봉', '쿨거 가능')
                    - 간결하고 핵심만, 조사와 어미 최소화
                    - 문장 끝에 마침표 없이 끊어서 작성""";
            case "SOFT" -> """
                    - 부드럽고 귀여운 말투 (예: '상태 좋아용~', '구경해보세용 ㅎㅎ')
                    - '~', 'ㅎㅎ', 'ㅋㅋ' 등 자연스러운 인터넷 표현 사용
                    - 살짝 애교 섞인 느낌""";
            default -> """
                    - 존댓말 사용 (예: '상태 좋아요', '확인해보세요')
                    - 정중하지만 딱딱하지 않게, 따뜻한 느낌
                    - '~요', '~해요' 체 사용""";
        };

        String personalityInstruction = switch (personality) {
            case "귀여움" -> "발랄하고 귀여운 분위기, 이모지와 귀여운 표현 많이 사용";
            case "깔끔함" -> "깔끔하고 정돈된 분위기, 군더더기 없이 핵심 정보 위주";
            default -> "친근하고 따뜻한 분위기, 이웃 간 대화하듯 편안하게";
        };

        return """
                너는 K-POP 아이돌 굿즈 중고거래 앱 '체리마켓'의 판매글 작성 도우미야.
                아래 정보를 바탕으로 실제 중고거래 앱에 올릴 판매글 본문을 작성해줘.

                [상품 정보]
                - 상품 키워드: %s
                - 카테고리: %s

                [작성 스타일]
                - 분위기: %s
                %s

                [작성 규칙]
                1. 150~250자 사이로 작성
                2. 제목은 쓰지 말고 본문만 작성
                3. 인사 → 상품 소개 → 상태/구성 → 거래 안내 순서로 자연스럽게 구성
                4. K-POP 팬들이 실제 쓰는 용어 활용 (양도, 쿨거, 컨디션, 미개봉, 실물, 택포 등)
                5. 이모지는 2~4개 정도만 포인트로 사용 (과하지 않게)
                6. 실제 판매자가 직접 쓴 것처럼 자연스럽게 (AI가 쓴 티 나지 않게)
                7. 허위 정보를 지어내지 말고, 키워드에 있는 내용만 활용
                """.formatted(
                        request.keywords(),
                        request.category(),
                        personalityInstruction,
                        toneInstruction
                );
    }

    private AiGenerateResponse callFallback(AiGenerateRequest request) {
        String text = """
                안녕하세요! %s 판매합니다 ✨
                상태 좋고 소중히 보관했어요.
                궁금한 점은 채팅 주세요! 🍒""".formatted(request.keywords());
        return new AiGenerateResponse(text, 0);
    }
}
