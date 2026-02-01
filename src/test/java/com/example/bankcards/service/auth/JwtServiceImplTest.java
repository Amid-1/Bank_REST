package com.example.bankcards.service.card;

import com.example.bankcards.service.auth.JwtServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceImplTest {

    @Test
    void parseSecret_rawPrefix_returnsUtf8Bytes() {
        byte[] b = JwtServiceImpl.parseSecret("raw:12345678901234567890123456789012");
        assertThat(b).hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void parseSecret_base64Prefix_decodes() {
        byte[] b = JwtServiceImpl.parseSecret("base64:MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");
        assertThat(b).hasSize(32);
    }

    @Test
    void parseSecret_defaultRaw_works() {
        byte[] b = JwtServiceImpl.parseSecret("12345678901234567890123456789012");
        assertThat(b).hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void parseSecret_tooShort_throws() {
        assertThatThrownBy(() -> JwtServiceImpl.parseSecret("raw:short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }
}
