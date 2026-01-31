package com.example.bankcards.util;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class SortParser {

    private SortParser() {
    }

    public static Sort parseSort(List<String> sort) {
        if (sort == null || sort.isEmpty()) return Sort.unsorted();

        List<Sort.Order> orders = new ArrayList<>();
        for (String raw : sort) {
            if (raw == null) continue;
            String s = raw.trim();
            if (s.isEmpty()) continue;

            String[] parts = s.split(",", 2);
            String property = parts[0].trim();
            if (property.isEmpty()) continue;

            Sort.Direction dir = Sort.Direction.ASC;
            if (parts.length == 2) {
                String d = parts[1].trim();
                if ("desc".equalsIgnoreCase(d)) dir = Sort.Direction.DESC;
                else if ("asc".equalsIgnoreCase(d)) dir = Sort.Direction.ASC;
            }

            orders.add(new Sort.Order(dir, property));
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
