package com.power.pipeline.silver.service;

import com.power.pipeline.bronze.entity.BronzePowerData;
import com.power.pipeline.bronze.repository.BronzePowerDataRepository;
import com.power.pipeline.common.util.DataParser;
import com.power.pipeline.silver.entity.SilverPowerData;
import com.power.pipeline.silver.repository.SilverPowerDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SilverProcessingService {

    private final BronzePowerDataRepository bronzeRepository;
    private final SilverPowerDataRepository silverRepository;
    private final DataParser dataParser;

    private static final Map<String, Integer> SOURCE_PRIORITY = Map.of(
            "POSOCO", 1,
            "SLDC_MAHARASHTRA", 2,
            "SLDC_GUJARAT", 2,
            "SLDC_GOA", 2
    );

    @Transactional
    public int processBronzeToSilver() {
        List<BronzePowerData> bronzeRecords = bronzeRepository.findAll();
        log.info("Processing {} bronze records to silver", bronzeRecords.size());

        int processed = 0;
        int skipped = 0;
        for (BronzePowerData bronze : bronzeRecords) {
            try {
                // Idempotency check: Skip if already processed
                boolean exists = silverRepository.existsByBronzeRecordId(bronze.getId());
                if (exists) {
                    log.warn("Bronze record {} (source: {}) already processed, skipping",
                             bronze.getId(), bronze.getDataSource());
                    skipped++;
                    continue;
                }
                log.info("Processing bronze record {} (source: {})", bronze.getId(), bronze.getDataSource());

                SilverPowerData silver = transformToSilver(bronze);
                if (silver != null && silver.getIsValid()) {
                    silverRepository.save(silver);
                    processed++;
                }
            } catch (Exception e) {
                log.error("Failed to process bronze record {}", bronze.getId(), e);
            }
        }

        log.info("Silver processing completed. Records processed: {}, skipped: {}", processed, skipped);
        return processed;
    }

    private SilverPowerData transformToSilver(BronzePowerData bronze) {
        String state = dataParser.standardizeStateName(bronze.getStateRaw());
        if (state == null) {
            log.warn("Could not standardize state name: {}", bronze.getStateRaw());
            return null;
        }

        Double energyRequired = dataParser.parseDouble(bronze.getRequirementRaw());
        Double energySupplied = dataParser.parseDouble(bronze.getSupplyRaw());
        Double energyShortfall = dataParser.parseDouble(bronze.getShortageRaw());
        Double shortfallPercentage = dataParser.parseDouble(bronze.getShortagePercentageRaw());

        // Validate and calculate quality
        List<String> qualityFlags = new ArrayList<>();
        double qualityScore = 1.0;

        // Check for missing values
        if (energyRequired == null) {
            qualityFlags.add("MISSING_REQUIREMENT");
            qualityScore -= 0.2;
        }
        if (energySupplied == null) {
            qualityFlags.add("MISSING_SUPPLY");
            qualityScore -= 0.2;
        }

        // Calculate shortfall if missing
        if (energyShortfall == null && energyRequired != null && energySupplied != null) {
            energyShortfall = energyRequired - energySupplied;
        }

        // Check for negative values
        if (energyRequired != null && energyRequired < 0) {
            qualityFlags.add("NEGATIVE_REQUIREMENT");
            qualityScore -= 0.2;
        }
        if (energySupplied != null && energySupplied < 0) {
            qualityFlags.add("NEGATIVE_SUPPLY");
            qualityScore -= 0.2;
        }

        // Check mathematical consistency
        if (energyRequired != null && energySupplied != null && energyShortfall != null) {
            double calculated = energyRequired - energySupplied;
            double diff = Math.abs(calculated - energyShortfall);
            if (diff > 0.1) { // Allow small floating point differences
                qualityFlags.add("MATH_INCONSISTENCY");
                qualityScore -= 0.3;
            }
        }

        // Calculate shortfall percentage if missing
        if (shortfallPercentage == null && energyRequired != null && energyShortfall != null && energyRequired > 0) {
            shortfallPercentage = (energyShortfall / energyRequired) * 100.0;
        }

        qualityScore = Math.max(0.0, Math.min(1.0, qualityScore));

        return SilverPowerData.builder()
                .dataSource(bronze.getDataSource())
                .state(state)
                .reportDate(bronze.getReportDate())
                .energyRequiredMu(energyRequired)
                .energySuppliedMu(energySupplied)
                .energyShortfallMu(energyShortfall)
                .shortfallPercentage(shortfallPercentage)
                .sourcePriority(SOURCE_PRIORITY.getOrDefault(bronze.getDataSource(), 99))
                .ingestionTimestamp(LocalDateTime.now())
                .bronzeRecordId(bronze.getId())
                .qualityScore(qualityScore)
                .qualityFlags(qualityFlags.isEmpty() ? null : String.join(",", qualityFlags))
                .isValid(qualityScore >= 0.5) // Threshold for validity
                .build();
    }
}
