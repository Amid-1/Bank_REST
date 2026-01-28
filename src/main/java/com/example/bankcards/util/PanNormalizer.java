package com.example.bankcards.util;

public final class PanNormalizer {
    private PanNormalizer() {}

    public static String normalize(String pan) {
        if (pan == null) throw new IllegalArgumentException("PAN is null");
        return pan.replaceAll("[^0-9]", "");
    }
}
