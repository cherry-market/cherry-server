package com.cherry.server.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cherry.server.auth.controller.AuthController;
import com.cherry.server.auth.dto.TokenResponse;
import com.cherry.server.auth.service.AuthService;
import com.cherry.server.global.common.GlobalExceptionHandler;
import com.cherry.server.security.JwtAccessDeniedHandler;
import com.cherry.server.security.JwtAuthenticationEntryPoint;
import com.cherry.server.security.JwtAuthenticationFilter;
import com.cherry.server.security.JwtTokenProvider;
import com.cherry.server.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        JwtTokenProvider.class,
        GlobalExceptionHandler.class
})
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private AuthService authService;

    @Test
    void login_is_permitted_for_anonymous() throws Exception {
        when(authService.login(any()))
                .thenReturn(new TokenResponse("token", null, "Bearer"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void login_invalid_credentials_returns_service_message() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }
}
