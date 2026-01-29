package com.example.bankcards.controller;

import com.example.bankcards.config.JacksonConfiguration;
import com.example.bankcards.config.SecurityConfiguration;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.exception.ApiExceptionHandler;
import com.example.bankcards.security.filter.JwtAuthFilter;
import com.example.bankcards.service.auth.JwtService;
import com.example.bankcards.service.card.CardsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardsController.class)
@Import({SecurityConfiguration.class, JacksonConfiguration.class, ApiExceptionHandler.class, JwtAuthFilter.class})
class UserCardControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CardsService cardsService;

    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    void getMyCards_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void getMyCards_userSeesOnlyOwnCards_filtersAndPageable_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser principal = AppUser.builder()
                .id(userId)
                .name("U")
                .email("u@mail.ru")
                .passwordHash("x")
                .role(UserRole.ROLE_USER)
                .build();

        UUID cardId = UUID.randomUUID();
        when(cardsService.getMyCards(eq(userId), eq(BankCardStatus.ACTIVE), eq("1234"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new CardResponse(cardId, "**** **** **** 1234", LocalDate.now().plusYears(1),
                                new BigDecimal("10.00"), BankCardStatus.ACTIVE, userId)
                )));

        mockMvc.perform(get("/api/cards")
                        .with(user(principal))
                        .param("status", "ACTIVE")
                        .param("last4", "1234")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(cardId.toString()))
                .andExpect(jsonPath("$.content[0].ownerId").value(userId.toString()));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cardsService).getMyCards(eq(userId), eq(BankCardStatus.ACTIVE), eq("1234"), pageableCaptor.capture());

        Pageable p = pageableCaptor.getValue();
        assertThat(p.getPageNumber()).isEqualTo(0);
        assertThat(p.getPageSize()).isEqualTo(5);
    }
}
