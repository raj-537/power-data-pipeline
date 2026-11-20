package com.power.pipeline.gold.repository;

import com.power.pipeline.gold.entity.GoldPowerData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoldPowerDataRepository extends JpaRepository<GoldPowerData, Long> {

    Optional<GoldPowerData> findByStateAndReportDate(String state, LocalDate reportDate);

    List<GoldPowerData> findByStateOrderByReportDateDesc(String state);

    List<GoldPowerData> findByReportDateBetween(LocalDate startDate, LocalDate endDate);

    // Plain SQL query - easier to understand
    @Query(value = "SELECT * FROM gold_power_data WHERE state IN :states " +
           "AND report_date BETWEEN :startDate AND :endDate " +
           "ORDER BY state, report_date",
           nativeQuery = true)
    List<GoldPowerData> findByStatesAndDateRange(
        @Param("states") List<String> states,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // Plain SQL query
    @Query(value = "SELECT * FROM gold_power_data WHERE confidence_level = :confidenceLevel",
           nativeQuery = true)
    List<GoldPowerData> findByConfidenceLevel(@Param("confidenceLevel") String confidenceLevel);

    // Plain SQL query with WHERE and ORDER BY
    @Query(value = "SELECT * FROM gold_power_data " +
           "WHERE secondary_source IS NOT NULL " +
           "AND source_agreement_percentage < :threshold " +
           "ORDER BY source_agreement_percentage ASC",
           nativeQuery = true)
    List<GoldPowerData> findRecordsWithSourceDisagreement(@Param("threshold") Double threshold);
}
