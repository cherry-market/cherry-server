package com.cherry.server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HandlerMappingIntrospector introspector
    ) throws Exception {
        MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);
        MvcRequestMatcher optionsMatcher = mvc.pattern("/**");
        optionsMatcher.setMethod(HttpMethod.OPTIONS);
        MvcRequestMatcher aiMatcher = mvc.pattern("/ai/**");
        aiMatcher.setMethod(HttpMethod.POST);
        MvcRequestMatcher healthMatcher = mvc.pattern("/health");
        MvcRequestMatcher likeStatusMatcher = mvc.pattern("/products/{productId}/like-status");
        likeStatusMatcher.setMethod(HttpMethod.GET);
        MvcRequestMatcher productsMatcher = mvc.pattern("/products/**");
        productsMatcher.setMethod(HttpMethod.GET);
        MvcRequestMatcher authMatcher = mvc.pattern("/auth/**");
        authMatcher.setMethod(HttpMethod.POST);

        http.cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(optionsMatcher).permitAll()
                        .requestMatchers(aiMatcher).permitAll()
                        .requestMatchers(healthMatcher).permitAll()
                        .requestMatchers(likeStatusMatcher).authenticated()
                        .requestMatchers(productsMatcher).permitAll()
                        .requestMatchers(authMatcher).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
