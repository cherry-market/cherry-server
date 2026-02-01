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
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadService uploadService;

    @Test
    void rejects_invalid_content_type() throws Exception {
        String payload = """
                {"files":[{"fileName":"a.gif","contentType":"image/gif","size":1024}]}
                """;

        mockMvc.perform(post("/api/upload/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
