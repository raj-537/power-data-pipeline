package com.power.pipeline.gold.service;

import com.power.pipeline.gold.entity.GoldPowerData;
import com.power.pipeline.gold.repository.GoldPowerDataRepository;
import com.power.pipeline.silver.entity.SilverPowerData;
import com.power.pipeline.silver.repository.SilverPowerDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoldAggregationService {

    private final SilverPowerDataRepository silverRepository;
    private final GoldPowerDataRepository goldRepository;

    @Transactional
    public int aggregateSilverToGold() {
        List<String> states = silverRepository.findDistinctStates();
        log.info("Aggregating silver data to gold for {} states", states.size());

        int aggregated = 0;

        for (String state : states) {
            List<SilverPowerData> stateRecords = silverRepository.findAll().stream()
                    .filter(s -> s.getState().equals(state) && s.getIsValid())
                    .collect(Collectors.toList());

            // Group by date
            var recordsByDate = stateRecords.stream()
                    .collect(Collectors.groupingBy(SilverPowerData::getReportDate));

            for (var entry : recordsByDate.entrySet()) {
                try {
                    GoldPowerData gold = aggregateForStateAndDate(state, entry.getKey(), entry.getValue());
                    if (gold != null) {
                        // Check if record already exists
                        var existing = goldRepository.findByStateAndReportDate(state, entry.getKey());
                        if (existing.isPresent()) {
                            // Update existing record
                            GoldPowerData existingGold = existing.get();
                            existingGold.setEnergyRequiredMu(gold.getEnergyRequiredMu());
                            existingGold.setEnergySuppliedMu(gold.getEnergySuppliedMu());
                            existingGold.setEnergyShortfallMu(gold.getEnergyShortfallMu());
                            existingGold.setShortfallPercentage(gold.getShortfallPercentage());
                            existingGold.setPrimarySource(gold.getPrimarySource());
                            existingGold.setSecondarySource(gold.getSecondarySource());
                            existingGold.setSourceAgreementPercentage(gold.getSourceAgreementPercentage());
                            existingGold.setDataQualityScore(gold.getDataQualityScore());
                            existingGold.setConfidenceLevel(gold.getConfidenceLevel());
                            existingGold.setQualityNotes(gold.getQualityNotes());
                            existingGold.setSilverRecordIds(gold.getSilverRecordIds());
                            goldRepository.save(existingGold);
                            log.debug("Updated existing gold record for state={}, date={}", state, entry.getKey());
                        } else {
                            // Create new record
                            goldRepository.save(gold);
                            log.debug("Created new gold record for state={}, date={}", state, entry.getKey());
                        }
                        aggregated++;
                    }
                } catch (Exception e) {
                    log.error("Failed to aggregate for state={}, date={}", state, entry.getKey(), e);
                }
            }
        }

        log.info("Gold aggregation completed. Records aggregated: {}", aggregated);
        return aggregated;
    }

    private GoldPowerData aggregateForStateAndDate(String state, LocalDate date,
                                                    List<SilverPowerData> silverRecords) {
        if (silverRecords.isEmpty()) {
            return null;
        }

        // Sort by priority and quality score
        silverRecords.sort((a, b) -> {
            int priorityCompare = Integer.compare(a.getSourcePriority(), b.getSourcePriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Double.compare(b.getQualityScore(), a.getQualityScore());
        });

        // Best record (highest priority, best quality)
        SilverPowerData best = silverRecords.get(0);
        SilverPowerData secondary = silverRecords.size() > 1 ? silverRecords.get(1) : null;

        // Calculate source agreement
        Double sourceAgreement = null;
        if (secondary != null && best.getEnergyRequiredMu() != null &&
                secondary.getEnergyRequiredMu() != null) {
            double diff = Math.abs(best.getEnergyRequiredMu() - secondary.getEnergyRequiredMu());
            double avg = (best.getEnergyRequiredMu() + secondary.getEnergyRequiredMu()) / 2.0;
            if (avg > 0) {
                sourceAgreement = (1.0 - (diff / avg)) * 100.0;
                sourceAgreement = Math.max(0.0, Math.min(100.0, sourceAgreement));
            }
        }

        // Calculate average quality score
        double avgQualityScore = silverRecords.stream()
                .mapToDouble(SilverPowerData::getQualityScore)
                .average()
                .orElse(0.0);

        // Determine confidence level
        String confidenceLevel = determineConfidenceLevel(silverRecords, sourceAgreement, avgQualityScore);

        // Collect quality notes
        String qualityNotes = buildQualityNotes(silverRecords, sourceAgreement);

        // Collect silver IDs
        String silverRecordIds = silverRecords.stream()
                .map(s -> String.valueOf(s.getId()))
                .collect(Collectors.joining(","));

        return GoldPowerData.builder()
                .state(state)
                .reportDate(date)
                .energyRequiredMu(best.getEnergyRequiredMu())
                .energySuppliedMu(best.getEnergySuppliedMu())
                .energyShortfallMu(best.getEnergyShortfallMu())
                .shortfallPercentage(best.getShortfallPercentage())
                .primarySource(best.getDataSource())
                .secondarySource(secondary != null ? secondary.getDataSource() : null)
                .sourceAgreementPercentage(sourceAgreement)
                .dataQualityScore(avgQualityScore)
                .confidenceLevel(confidenceLevel)
                .qualityNotes(qualityNotes)
                .silverRecordIds(silverRecordIds)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String determineConfidenceLevel(List<SilverPowerData> records,
                                           Double sourceAgreement,
                                           double avgQualityScore) {
        if (avgQualityScore >= 0.9) {
            if (records.size() > 1 && sourceAgreement != null && sourceAgreement >= 95.0) {
                return "HIGH";
            }
            if (records.size() == 1 && records.get(0).getSourcePriority() == 1) {
                return "HIGH";
            }
            return "MEDIUM";
        } else if (avgQualityScore >= 0.7) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String buildQualityNotes(List<SilverPowerData> records, Double sourceAgreement) {
        StringBuilder notes = new StringBuilder();

        if (records.size() == 1) {
            notes.append("Single source: ").append(records.get(0).getDataSource()).append(". ");
        } else {
            notes.append("Multiple sources (").append(records.size()).append("): ");
            String sources = records.stream()
                    .map(SilverPowerData::getDataSource)
                    .collect(Collectors.joining(", "));
            notes.append(sources).append(". ");
        }

        if (sourceAgreement != null) {
            notes.append(String.format("Source agreement: %.2f%%. ", sourceAgreement));
        }

        // Check for quality flags
        long recordsWithFlags = records.stream()
                .filter(r -> r.getQualityFlags() != null && !r.getQualityFlags().isEmpty())
                .count();

        if (recordsWithFlags > 0) {
            notes.append(recordsWithFlags).append(" record(s) have quality issues. ");
        }

        return notes.toString().trim();
    }
}
