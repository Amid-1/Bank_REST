package com.example.bankcards.util;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {}

    public static String normalize(String email) {
        if (email == null) throw new IllegalArgumentException("email is null");
        String e = email.trim().toLowerCase(Locale.ROOT);
        if (e.isEmpty()) throw new IllegalArgumentException("email is blank");
        return e;
    }
}
