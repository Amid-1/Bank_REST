package com.example.bankcards.controller;

import com.example.bankcards.config.JacksonConfiguration;
import com.example.bankcards.config.SecurityConfiguration;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.exception.ApiExceptionHandler;
import com.example.bankcards.security.filter.JwtAuthFilter;
import com.example.bankcards.service.auth.JwtService;
import com.example.bankcards.service.card.CardsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminCardsController.class)
@Import({SecurityConfiguration.class, JacksonConfiguration.class, ApiExceptionHandler.class, JwtAuthFilter.class})
class AdminCardControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean
    CardsService cardsService;

    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void adminCards_userRole_forbidden403() throws Exception {
        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_returns200() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(cardsService.createCard(any())).thenReturn(new CardResponse(
                cardId,
                "**** **** **** 1111",
                LocalDate.now().plusYears(5),
                new BigDecimal("0.00"),
                BankCardStatus.ACTIVE,
                ownerId
        ));

        String body = """
                {"cardNumber":"4111111111111111","expirationDate":"2035-08-11","ownerId":"%s"}
                """.formatted(ownerId);

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 1111"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_duplicatePan_returns409() throws Exception {
        when(cardsService.createCard(any()))
                .thenThrow(new IllegalStateException("Карта уже существует (дубликат panHash)"));

        String body = """
                {"cardNumber":"4111111111111111","expirationDate":"2035-08-11","ownerId":"%s"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Карта уже существует (дубликат panHash)"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateExpired_returns409() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardsService.activateCard(eq(cardId)))
                .thenThrow(new IllegalStateException("Нельзя активировать просроченную карту: " + cardId));

        mockMvc.perform(patch("/api/admin/cards/{id}/activate", cardId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Нельзя активировать просроченную карту: " + cardId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void search_pageableAndFilters_returns200AndContent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        var page = new PageImpl<>(
                List.of(new CardResponse(
                        cardId,
                        "**** **** **** 1234",
                        LocalDate.now().plusYears(1),
                        new BigDecimal("10.00"),
                        BankCardStatus.ACTIVE,
                        ownerId
                )),
                PageRequest.of(0, 20),
                1
        );

        when(cardsService.searchCards(eq(ownerId), eq(BankCardStatus.ACTIVE), eq("1234"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/cards")
                        .param("ownerId", ownerId.toString())
                        .param("status", "ACTIVE")
                        .param("last4", "1234")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(cardId.toString()))
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 1234"));
    }
}
