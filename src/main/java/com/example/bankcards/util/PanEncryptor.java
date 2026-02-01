package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PanEncryptor {

    private static final String AES = "AES";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LEN_BYTES = 12;
    private static final int TAG_LEN_BITS = 128;
    private static final int KEY_LEN_BITS = 256;
    private static final int PBKDF2_ITERS = 120_000;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public PanEncryptor(
            @Value("${encryptor.password}") String password,
            @Value("${encryptor.salt}") String salt
    ) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Не задан параметр шифрования (password)");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalStateException("Не задан параметр шифрования (salt)");
        }
        this.key = deriveKey(password, salt);
    }

    public String encrypt(String panNormalized) {
        if (panNormalized == null || panNormalized.isBlank()) {
            throw new IllegalArgumentException("Номер (PAN) карты пустой");
        }
        try {
            byte[] iv = new byte[IV_LEN_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));

            byte[] ciphertext = cipher.doFinal(panNormalized.getBytes(StandardCharsets.UTF_8));

            String ivB64 = Base64.getEncoder().encodeToString(iv);
            String ctB64 = Base64.getEncoder().encodeToString(ciphertext);
            return ivB64 + ":" + ctB64;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось зашифровать номер карты (PAN)", e);
        }
    }

    private static SecretKey deriveKey(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8),
                    PBKDF2_ITERS,
                    KEY_LEN_BITS
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, AES);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сформировать ключ шифрования", e);
        }
    }
}
