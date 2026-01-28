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
    private static final int IV_LEN_BYTES = 12;       // стандарт для GCM
    private static final int TAG_LEN_BITS = 128;      // стандарт
    private static final int KEY_LEN_BITS = 256;
    private static final int PBKDF2_ITERS = 120_000;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public PanEncryptor(
            @Value("${encryptor.password}") String password,
            @Value("${encryptor.salt}") String salt
    ) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("encryptor.password must be set");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalStateException("encryptor.salt must be set");
        }
        this.key = deriveKey(password, salt);
    }

    public String encrypt(String panNormalized) {
        try {
            byte[] iv = new byte[IV_LEN_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));

            byte[] plaintext = panNormalized.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintext);

            return b64(iv) + ":" + b64(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot encrypt PAN", e);
        }
    }

    public String decrypt(String stored) {
        try {
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted format, expected base64(iv):base64(ciphertext)");
            }

            byte[] iv = b64d(parts[0]);
            byte[] ciphertext = b64d(parts[1]);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot decrypt PAN", e);
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
            throw new IllegalStateException("Cannot derive encryption key", e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] b64d(String s) {
        return Base64.getDecoder().decode(s);
    }
}
