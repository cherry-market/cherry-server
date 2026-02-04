package com.cherry.server.upload;

import com.cherry.server.upload.dto.ImageCallbackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/images")
@RequiredArgsConstructor
public class ImageCallbackController {

    private final ImageCallbackService imageCallbackService;

    @Value("${internal.token:}")
    private String internalToken;

    @PostMapping("/complete")
    public ResponseEntity<Void> complete(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody ImageCallbackRequest request
    ) {
        if (internalToken == null || internalToken.isBlank() || token == null || !internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal token");
        }
        imageCallbackService.apply(request);
        return ResponseEntity.noContent().build();
    }
}
