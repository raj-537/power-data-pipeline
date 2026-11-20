package com.power.pipeline.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DataParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    public String standardizeStateName(String stateName) {
        if (stateName == null) {
            return null;
        }

        String normalized = stateName.trim().toUpperCase();

        return switch (normalized) {
            case "MAHARASHTRA" -> "Maharashtra";
            case "GUJARAT" -> "Gujarat";
            case "GOA" -> "Goa";
            case "KARNATAKA" -> "Karnataka";
            case "TAMIL NADU", "TAMILNADU" -> "Tamil Nadu";
            case "DELHI", "NCT OF DELHI" -> "Delhi";
            default -> capitalize(stateName.trim());
        };
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
