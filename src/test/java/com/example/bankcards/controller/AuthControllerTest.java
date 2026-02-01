package com.example.bankcards.controller;

import com.example.bankcards.config.JacksonConfiguration;
import com.example.bankcards.config.SecurityConfiguration;
import com.example.bankcards.dto.auth.JwtResponse;
import com.example.bankcards.exception.ApiExceptionHandler;
import com.example.bankcards.security.filter.JwtAuthFilter;
import com.example.bankcards.service.auth.AuthService;
import com.example.bankcards.service.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthenticationController.class)
@Import({SecurityConfiguration.class, JacksonConfiguration.class, ApiExceptionHandler.class, JwtAuthFilter.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthService authService;

    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    void login_validCredentials_returns200AndToken() throws Exception {
        when(authService.login(any())).thenReturn(new JwtResponse("token-123"));

        String body = """
                {"email":"user@mail.ru","password":"qwerty_best_password"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("token-123"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Неверный логин или пароль"));

        String body = """
                {"email":"user@mail.ru","password":"wrong_password"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Неверный логин или пароль"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void login_invalidEmail_returns400() throws Exception {
        String body = """
            {"email":"not-email","password":"qwerty_best_password"}
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Ошибка валидации"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations[0].field").value("email"));
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        String body = """
            {"email":"user@mail.ru","password":""}
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Ошибка валидации"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations[0].field").value("password"));
    }

}
