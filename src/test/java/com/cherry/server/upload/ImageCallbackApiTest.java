package com.cherry.server.upload;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ImageCallbackApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageCallbackService imageCallbackService;

    @Test
    void rejects_missing_internal_token() throws Exception {
        String payload = """
                {"imageKey":"products/original/1.jpg","detailUrl":"d","thumbnailUrl":"t","imageOrder":0,"isThumbnail":true}
                """;

        mockMvc.perform(post("/internal/images/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }
}
