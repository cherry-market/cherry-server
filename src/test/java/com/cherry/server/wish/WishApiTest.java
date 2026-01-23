package com.cherry.server.wish;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cherry.server.global.common.GlobalExceptionHandler;
import com.cherry.server.security.JwtAccessDeniedHandler;
import com.cherry.server.security.JwtAuthenticationEntryPoint;
import com.cherry.server.security.JwtAuthenticationFilter;
import com.cherry.server.security.JwtTokenProvider;
import com.cherry.server.security.SecurityConfig;
import com.cherry.server.wish.controller.WishController;
import com.cherry.server.wish.service.WishService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WishController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        JwtTokenProvider.class,
        GlobalExceptionHandler.class
})
class WishApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private WishService wishService;

    @Test
    void add_like_returns_ok_for_authenticated() throws Exception {
        mockMvc.perform(post("/products/1/like")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void remove_like_returns_no_content_for_authenticated() throws Exception {
        mockMvc.perform(delete("/products/1/like")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void like_status_returns_boolean_for_authenticated() throws Exception {
        when(wishService.isLiked(1L, 1L)).thenReturn(true);

        mockMvc.perform(get("/products/1/like-status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void my_likes_requires_authentication() throws Exception {
        mockMvc.perform(get("/me/likes"))
                .andExpect(status().isUnauthorized());
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.generateAccessToken(1L, "user@example.com");
    }
}
