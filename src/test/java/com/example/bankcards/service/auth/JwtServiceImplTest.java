package com.example.bankcards.service.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceImplTest {

    @Test
    void parseSecret_rawPrefix_returnsUtf8Bytes() {
        byte[] b = JwtServiceImpl.parseSecret("raw:12345678901234567890123456789012");
        assertThat(b).hasSize(32);
    }

    @Test
    void parseSecret_base64Prefix_decodes() {
        byte[] b = JwtServiceImpl.parseSecret("base64:MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");
        assertThat(b).hasSize(32);
    }

    @Test
    void parseSecret_defaultRaw_works() {
        byte[] b = JwtServiceImpl.parseSecret("12345678901234567890123456789012");
        assertThat(b).hasSize(32);
    }

    @Test
    void parseSecret_nullOrBlank_throws() {
        assertThatThrownBy(() -> JwtServiceImpl.parseSecret(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Секрет JWT не задан (jwt.secret пустой)");

        assertThatThrownBy(() -> JwtServiceImpl.parseSecret("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Секрет JWT не задан (jwt.secret пустой)");
    }

    @Test
    void parseSecret_base64Prefix_emptyPayload_throws() {
        assertThatThrownBy(() -> JwtServiceImpl.parseSecret("base64:   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Секрет JWT в формате base64 задан пустым");
    }

    @Test
    void parseSecret_rawPrefix_emptyPayload_throws() {
        assertThatThrownBy(() -> JwtServiceImpl.parseSecret("raw:   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Секрет JWT в формате raw задан пустым");
    }

    @Test
    void parseSecret_base64Prefix_invalidPayload_throwsIllegalState_withCause() {
        assertThatThrownBy(() -> JwtServiceImpl.parseSecret("base64:%%%not_base64%%%"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Секрет JWT в формате base64 задан некорректно")
                .hasCauseInstanceOf(RuntimeException.class); // cause = DecodingException
    }

    @Test
    void constructor_tooShortSecret_throws() {
        assertThatThrownBy(() -> new JwtServiceImpl("raw:short", 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Секрет JWT слишком короткий: для HS256 нужно минимум 32 байт, получено 5 байт");
    }
}
