package com.power.pipeline.bronze.repository;

import com.power.pipeline.bronze.entity.BronzePowerData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BronzePowerDataRepository extends JpaRepository<BronzePowerData, Long> {

    List<BronzePowerData> findByDataSourceAndReportDate(String dataSource, LocalDate reportDate);

    List<BronzePowerData> findByReportDateBetween(LocalDate startDate, LocalDate endDate);

    Optional<BronzePowerData> findByFileChecksum(String fileChecksum);

    // Plain SQL query - get all unique dates
    @Query(value = "SELECT DISTINCT report_date FROM bronze_power_data ORDER BY report_date DESC",
           nativeQuery = true)
    List<LocalDate> findDistinctDates();

    // Plain SQL query - count records in a batch
    @Query(value = "SELECT COUNT(*) FROM bronze_power_data WHERE ingestion_batch_id = :batchId",
           nativeQuery = true)
    Long countByBatchId(@Param("batchId") String batchId);
}
