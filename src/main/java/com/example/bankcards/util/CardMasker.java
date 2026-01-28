package com.example.bankcards.util;

public final class CardMasker {

    private CardMasker() {}

    public static String mask(String panNormalized) {
        if (panNormalized == null) {
            throw new IllegalArgumentException("PAN is null");
        }
        String pan = panNormalized.replaceAll("\\s+", "");
        if (pan.length() < 4) {
            throw new IllegalArgumentException("PAN must have at least 4 digits");
        }
        String last4 = pan.substring(pan.length() - 4);
        return "**** **** **** " + last4;
    }
}
