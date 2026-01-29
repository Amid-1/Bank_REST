package com.example.bankcards.controller;

import com.example.bankcards.config.JacksonConfiguration;
import com.example.bankcards.config.SecurityConfiguration;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.exception.ApiExceptionHandler;
import com.example.bankcards.security.filter.JwtAuthFilter;
import com.example.bankcards.service.auth.JwtService;
import com.example.bankcards.service.user.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUsersController.class)
@Import({SecurityConfiguration.class, JacksonConfiguration.class, ApiExceptionHandler.class, JwtAuthFilter.class})
class AdminUserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UsersService usersService;

    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoints_userRole_forbidden403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_adminRole_returns201AndBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(usersService.create(any())).thenReturn(new UserResponse(id, "Andy", "andy@mail.ru"));

        String body = """
                {"name":"Andy","email":"andy@mail.ru","password":"very_strong_password"}
                """;

        mockMvc.perform(post("/api/admin/users")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Andy"))
                .andExpect(jsonPath("$.email").value("andy@mail.ru"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_duplicateEmail_returns409() throws Exception {
        when(usersService.create(any()))
                .thenThrow(new IllegalStateException("Пользователь с таким email уже существует"));

        String body = """
                {"name":"Andy","email":"andy@mail.ru","password":"very_strong_password"}
                """;

        mockMvc.perform(post("/api/admin/users")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Пользователь с таким email уже существует"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRole_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(usersService.updateRole(eq(id), any()))
                .thenReturn(new UserResponse(id, "Andy", "andy@mail.ru"));

        String body = """
                {"role":"ROLE_ADMIN"}
                """;

        mockMvc.perform(patch("/api/admin/users/{id}/role", id)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateEnabled_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(usersService.updateEnabled(eq(id), any()))
                .thenReturn(new UserResponse(id, "Andy", "andy@mail.ru"));

        String body = """
                {"enabled":false}
                """;

        mockMvc.perform(patch("/api/admin/users/{id}/enabled", id)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resetPassword_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        String body = """
                {"newPassword":"new_strong_password_123"}
                """;

        mockMvc.perform(patch("/api/admin/users/{id}/password", id)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/users/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new jakarta.persistence.EntityNotFoundException("Пользователь не найден: " + id))
                .when(usersService).delete(eq(id));

        mockMvc.perform(delete("/api/admin/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Пользователь не найден: " + id));
    }
}
