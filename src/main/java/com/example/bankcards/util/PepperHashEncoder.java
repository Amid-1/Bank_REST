package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class PepperHashEncoder {

    private final String pepper;

    public PepperHashEncoder(@Value("${app.hashEncoder.pepper}") String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("app.hashEncoder.pepper must be set");
        }
        this.pepper = pepper;
    }

    public String sha256Hex(String panNormalized) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((panNormalized + pepper).getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash PAN", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = digits[v >>> 4];
            hex[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(hex);
    }
}
