package com.power.pipeline.api.service;

import com.power.pipeline.gold.entity.GoldPowerData;
import com.power.pipeline.gold.repository.GoldPowerDataJdbcRepository;
import com.power.pipeline.gold.repository.GoldPowerDataJdbcRepository.StateShortageStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Example service using JdbcTemplate repository
 *
 * This shows how easy it is to use plain SQL!
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoldDataJdbcService {

    // Inject JdbcTemplate repository
    private final GoldPowerDataJdbcRepository goldJdbcRepo;

    /**
     * Get all data for a state
     */
    public List<GoldPowerData> getStateData(String state) {
        log.info("Fetching data for state: {}", state);
        return goldJdbcRepo.findByState(state);
    }

    /**
     * Get data for specific state and date
     */
    public Optional<GoldPowerData> getStateDataForDate(String state, LocalDate date) {
        return goldJdbcRepo.findByStateAndReportDate(state, date);
    }

    /**
     * Get all high confidence records
     */
    public List<GoldPowerData> getHighConfidenceRecords() {
        return goldJdbcRepo.findByConfidenceLevel("HIGH");
    }

    /**
     * Get records with source disagreements
     */
    public List<GoldPowerData> getSourceDisagreements(Double threshold) {
        return goldJdbcRepo.findRecordsWithSourceDisagreement(threshold);
    }

    /**
     * Get shortage statistics for all states
     */
    public List<StateShortageStats> getShortageStatistics() {
        return goldJdbcRepo.getAverageShortageByState();
    }

    /**
     * Get data quality issues
     */
    public List<GoldPowerData> getQualityIssues() {
        return goldJdbcRepo.findQualityIssues();
    }

    /**
     * Get total record count
     */
    public Long getTotalRecords() {
        return goldJdbcRepo.count();
    }

    /**
     * Get count by confidence level
     */
    public Long getRecordCountByConfidence(String confidenceLevel) {
        return goldJdbcRepo.countByConfidenceLevel(confidenceLevel);
    }

    /**
     * Example: Get summary statistics
     */
    public SummaryStats getSummaryStatistics() {
        Long total = goldJdbcRepo.count();
        Long highConfidence = goldJdbcRepo.countByConfidenceLevel("HIGH");
        Long mediumConfidence = goldJdbcRepo.countByConfidenceLevel("MEDIUM");
        Long lowConfidence = goldJdbcRepo.countByConfidenceLevel("LOW");

        List<StateShortageStats> stateStats = goldJdbcRepo.getAverageShortageByState();

        return new SummaryStats(
            total,
            highConfidence,
            mediumConfidence,
            lowConfidence,
            stateStats
        );
    }

    // Result class
    public record SummaryStats(
        Long totalRecords,
        Long highConfidence,
        Long mediumConfidence,
        Long lowConfidence,
        List<StateShortageStats> stateStatistics
    ) {}
}
