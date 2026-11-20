package com.power.pipeline.silver.repository;

import com.power.pipeline.silver.entity.SilverPowerData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SilverPowerDataRepository extends JpaRepository<SilverPowerData, Long> {

    List<SilverPowerData> findByStateAndReportDate(String state, LocalDate reportDate);

    List<SilverPowerData> findByReportDateBetweenAndIsValid(
        LocalDate startDate,
        LocalDate endDate,
        Boolean isValid
    );

    // Plain SQL query - find valid records sorted by priority and quality
    @Query(value = "SELECT * FROM silver_power_data WHERE state = :state " +
           "AND report_date = :reportDate AND is_valid = true " +
           "ORDER BY source_priority ASC, quality_score DESC",
           nativeQuery = true)
    List<SilverPowerData> findValidRecordsForGoldAggregation(
        @Param("state") String state,
        @Param("reportDate") LocalDate reportDate
    );

    // Plain SQL query - get unique states
    @Query(value = "SELECT DISTINCT state FROM silver_power_data ORDER BY state",
           nativeQuery = true)
    List<String> findDistinctStates();

    // Check if bronze record already processed (for idempotency)
    boolean existsByBronzeRecordId(Long bronzeRecordId);
}
