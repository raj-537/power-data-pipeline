package com.power.pipeline.bronze.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.power.pipeline.bronze.entity.BronzePowerData;
import com.power.pipeline.bronze.repository.BronzePowerDataRepository;
import com.power.pipeline.common.util.DataParser;
import com.power.pipeline.common.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BronzeIngestionService {

    private final BronzePowerDataRepository bronzeRepository;
    private final FileUtil fileUtil;
    private final DataParser dataParser;

    @Value("${pipeline.data.raw-path:data/raw}")
    private String rawDataPath;

    @Transactional
    public int ingestRawFiles() {
        File rawDir = new File(rawDataPath);
        if (!rawDir.exists() || !rawDir.isDirectory()) {
            log.error("Raw data directory not found: {}", rawDataPath);
            return 0;
        }

        File[] files = rawDir.listFiles((dir, name) -> name.endsWith(".csv"));
        if (files == null || files.length == 0) {
            log.warn("No CSV files found in: {}", rawDataPath);
            return 0;
        }

        String batchId = "batch_" + UUID.randomUUID().toString().substring(0, 8);
        int totalIngested = 0;

        for (File file : files) {
            try {
                int count = ingestFile(file, batchId);
                totalIngested += count;
                log.info("Ingested {} records from {}", count, file.getName());
            } catch (Exception e) {
                log.error("Failed to ingest file: {}", file.getName(), e);
            }
        }

        log.info("Bronze ingestion completed. Total records: {}, Batch ID: {}", totalIngested, batchId);
        return totalIngested;
    }

    @Transactional
    public int ingestFile(File file, String batchId) throws IOException, CsvException {
        String checksum = fileUtil.calculateChecksum(file);

        // Check if already ingested
        if (bronzeRepository.findByFileChecksum(checksum).isPresent()) {
            log.info("File already ingested (checksum match): {}", file.getName());
            return 0;
        }

        String dataSource = determineDataSource(file.getName());
        List<BronzePowerData> records = parseFile(file, dataSource, checksum, batchId);

        if (records.isEmpty()) {
            log.warn("No records parsed from file: {}", file.getName());
            return 0;
        }

        bronzeRepository.saveAll(records);
        return records.size();
    }

    private List<BronzePowerData> parseFile(File file, String dataSource, String checksum, String batchId)
            throws IOException, CsvException {

        List<BronzePowerData> records = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                return records;
            }

            String[] headers = rows.get(0);
            int dateIdx = findHeaderIndex(headers, "Date");
            int stateIdx = findHeaderIndex(headers, "State");

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                BronzePowerData record = buildBronzeRecord(row, headers, dataSource, file.getName(), checksum, batchId);
                if (record != null) {
                    records.add(record);
                }
            }
        }

        return records;
    }

    private BronzePowerData buildBronzeRecord(String[] row, String[] headers,
                                               String dataSource, String fileName,
                                               String checksum, String batchId) {
        try {
            int dateIdx = findHeaderIndex(headers, "Date");
            int stateIdx = findHeaderIndex(headers, "State");

            if (dateIdx < 0 || dateIdx >= row.length) {
                return null;
            }

            String dateStr = row[dateIdx];
            var reportDate = dataParser.parseDate(dateStr);
            if (reportDate == null) {
                return null;
            }

            // Get state from column or extract from filename (for SLDC files)
            String stateRaw = stateIdx >= 0 && stateIdx < row.length ? row[stateIdx] : null;
            if (stateRaw == null || stateRaw.trim().isEmpty()) {
                stateRaw = extractStateFromFilename(fileName, dataSource);
            }

            return BronzePowerData.builder()
                    .dataSource(dataSource)
                    .sourceFile(fileName)
                    .reportDate(reportDate)
                    .stateRaw(stateRaw)
                    .requirementRaw(getFieldValue(row, headers, "Requirement_MU", "Energy_Requirement_MU"))
                    .supplyRaw(getFieldValue(row, headers, "Availability_MU", "Energy_Met_MU", "Energy_Supplied_MU", "Met_MU"))
                    .shortageRaw(getFieldValue(row, headers, "Shortage_MU", "Energy_Deficit_MU", "Deficit_MW"))
                    .shortagePercentageRaw(getFieldValue(row, headers, "Shortage_Percentage"))
                    .ingestionTimestamp(LocalDateTime.now())
                    .fileChecksum(checksum)
                    .ingestionBatchId(batchId)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse row: {}", String.join(",", row), e);
            return null;
        }
    }

    private String getFieldValue(String[] row, String[] headers, String... possibleNames) {
        for (String name : possibleNames) {
            int idx = findHeaderIndex(headers, name);
            if (idx >= 0 && idx < row.length && row[idx] != null && !row[idx].trim().isEmpty()) {
                return row[idx].trim();
            }
        }
        return null;
    }

    private int findHeaderIndex(String[] headers, String headerName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(headerName) ||
                headers[i].replace("_", "").equalsIgnoreCase(headerName.replace("_", ""))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Extract state name from filename for SLDC files
     * Example: sldc_maharashtra_2026-11-13.csv → Maharashtra
     */
    private String extractStateFromFilename(String filename, String dataSource) {
        if (dataSource != null && dataSource.startsWith("SLDC_")) {
            // Filename pattern: sldc_maharashtra_2026-11-13_to_2026-11-19.csv
            String lower = filename.toLowerCase().replace(".csv", "");
            String[] parts = lower.split("_");

            // Find the state part (comes after "sldc")
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("sldc") && i + 1 < parts.length) {
                    String state = parts[i + 1];
                    // Capitalize first letter: maharashtra → Maharashtra
                    return state.substring(0, 1).toUpperCase() + state.substring(1);
                }
            }
        }
        return null;
    }

    private String determineDataSource(String filename) {
        String lower = filename.toLowerCase();
        if (lower.contains("posoco")) {
            return "POSOCO";
        } else if (lower.contains("maharashtra")) {
            return "SLDC_MAHARASHTRA";
        } else if (lower.contains("gujarat")) {
            return "SLDC_GUJARAT";
        } else if (lower.contains("goa")) {
            return "SLDC_GOA";
        }
        return "UNKNOWN";
    }
}
