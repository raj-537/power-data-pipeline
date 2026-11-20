package com.power.pipeline.common.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class FileUtil {

    public String calculateChecksum(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to calculate file checksum", e);
        }
    }

    public String extractDateFromFilename(String filename) {
        // Pattern: source_state_YYYY-MM-DD_to_YYYY-MM-DD.csv
        // Returns first date found
        String[] parts = filename.split("_");
        for (String part : parts) {
            if (part.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return part;
            }
        }
        return null;
    }
}
