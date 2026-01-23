package com.cherry.server.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GeminiImageService {

    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.api-version:v1beta}")
    private String apiVersion;

    @Value("${gemini.model:gemini-2.5-flash-image}")
    private String model;

    public Optional<String> generateGoodsImage(String promptDescription, String aspectRatio) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String prompt = """
                Professional product photography of: %s.
                Style: K-Pop merchandise aesthetic, high-end, clean, vibrant, studio lighting.
                No text overlays, no distorted hands, photorealistic 8k.
                """.formatted(promptDescription);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", prompt))
                        )
                ),
                "generationConfig", Map.of(
                        "imageConfig", Map.of(
                                "aspectRatio", aspectRatio
                        )
                )
        );

        String url = "%s/%s/models/%s:generateContent".formatted(
                trimTrailingSlash(baseUrl),
                apiVersion,
                model
        );

        try {
            String responseBody = RestClient.create()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(API_KEY_HEADER, apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return Optional.empty();
            }

            return extractDataUrl(responseBody);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini request failed.");
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini response parse failed.");
        }
    }

    private Optional<String> extractDataUrl(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return Optional.empty();
        }
        for (JsonNode part : parts) {
            JsonNode inlineData = part.path("inlineData");
            if (!inlineData.isMissingNode()) {
                String data = inlineData.path("data").asText(null);
                if (data == null || data.isBlank()) {
                    continue;
                }
                String mimeType = inlineData.path("mimeType").asText("image/png");
                return Optional.of("data:%s;base64,%s".formatted(mimeType, data));
            }
        }
        return Optional.empty();
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
