package com.cherry.server.upload.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cherry.server.upload.storage.StorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LocalUploadControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void uploads_file_to_local_storage() throws Exception {
        StorageProperties properties = new StorageProperties(
                "local",
                null,
                null,
                "http://localhost:8080/uploads",
                0,
                tempDir.toString(),
                "http://localhost:8080/api/upload/images"
        );
        LocalUploadStorage storage = new LocalUploadStorage(properties);
        LocalUploadController controller = new LocalUploadController(storage);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        byte[] payload = "data".getBytes();
        mockMvc.perform(put("/api/upload/images")
                        .param("imageKey", "products/original/1.jpg")
                        .contentType(MediaType.IMAGE_JPEG)
                        .content(payload))
                .andExpect(status().isNoContent());

        Path saved = tempDir.resolve("products/original/1.jpg");
        assertThat(Files.exists(saved)).isTrue();
    }
}

