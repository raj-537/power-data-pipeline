package com.power.pipeline.bronze.repository;

import com.power.pipeline.bronze.entity.BronzePowerData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BronzePowerDataJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    // RowMapper - converts SQL result to Java object
    private final RowMapper<BronzePowerData> rowMapper = (rs, rowNum) ->
        BronzePowerData.builder()
            .id(rs.getLong("id"))
            .dataSource(rs.getString("data_source"))
            .sourceFile(rs.getString("source_file"))
            .reportDate(rs.getDate("report_date").toLocalDate())
            .stateRaw(rs.getString("state_raw"))
            .requirementRaw(rs.getString("requirement_raw"))
            .supplyRaw(rs.getString("supply_raw"))
            .shortageRaw(rs.getString("shortage_raw"))
            .shortagePercentageRaw(rs.getString("shortage_percentage_raw"))
            .additionalFields(rs.getString("additional_fields"))
            .fileChecksum(rs.getString("file_checksum"))
            .ingestionBatchId(rs.getString("ingestion_batch_id"))
            .build();

    /**
     * Find all records
     */
    public List<BronzePowerData> findAll() {
        String sql = "SELECT * FROM bronze_power_data ORDER BY ingestion_timestamp DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Find by data source and report date
     */
    public List<BronzePowerData> findByDataSourceAndReportDate(
            String dataSource, LocalDate reportDate) {

        String sql = """
            SELECT * FROM bronze_power_data
            WHERE data_source = ?
              AND report_date = ?
            """;

        return jdbcTemplate.query(sql, rowMapper, dataSource, reportDate);
    }

    /**
     * Find by date range
     */
    public List<BronzePowerData> findByReportDateBetween(
            LocalDate startDate, LocalDate endDate) {

        String sql = """
            SELECT * FROM bronze_power_data
            WHERE report_date BETWEEN ? AND ?
            ORDER BY report_date
            """;

        return jdbcTemplate.query(sql, rowMapper, startDate, endDate);
    }

    /**
     * Find by file checksum (for idempotency check)
     */
    public Optional<BronzePowerData> findByFileChecksum(String fileChecksum) {
        String sql = "SELECT * FROM bronze_power_data WHERE file_checksum = ? LIMIT 1";

        List<BronzePowerData> results = jdbcTemplate.query(sql, rowMapper, fileChecksum);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Check if file already ingested (idempotency check)
     */
    public boolean existsByFileChecksum(String fileChecksum) {
        String sql = "SELECT COUNT(*) FROM bronze_power_data WHERE file_checksum = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, fileChecksum);
        return count != null && count > 0;
    }

    /**
     * Get all distinct dates
     */
    public List<LocalDate> findDistinctDates() {
        String sql = "SELECT DISTINCT report_date FROM bronze_power_data ORDER BY report_date DESC";
        return jdbcTemplate.queryForList(sql, LocalDate.class);
    }

    /**
     * Count records by batch ID
     */
    public Long countByBatchId(String batchId) {
        String sql = "SELECT COUNT(*) FROM bronze_power_data WHERE ingestion_batch_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, batchId);
    }

    /**
     * Count all records
     */
    public Long count() {
        String sql = "SELECT COUNT(*) FROM bronze_power_data";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * Get ingestion statistics by data source
     */
    public List<IngestionStats> getIngestionStatsBySource() {
        String sql = """
            SELECT data_source,
                   COUNT(*) as record_count,
                   COUNT(DISTINCT file_checksum) as unique_files,
                   MIN(report_date) as earliest_date,
                   MAX(report_date) as latest_date
            FROM bronze_power_data
            GROUP BY data_source
            ORDER BY record_count DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new IngestionStats(
            rs.getString("data_source"),
            rs.getLong("record_count"),
            rs.getLong("unique_files"),
            rs.getDate("earliest_date").toLocalDate(),
            rs.getDate("latest_date").toLocalDate()
        ));
    }

    /**
     * Save a bronze record
     */
    public int save(BronzePowerData bronze) {
        String sql = """
            INSERT INTO bronze_power_data (
                data_source, source_file, report_date, state_raw, requirement_raw,
                supply_raw, shortage_raw, shortage_percentage_raw, additional_fields,
                file_checksum, ingestion_batch_id, ingestion_timestamp
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        return jdbcTemplate.update(sql,
            bronze.getDataSource(),
            bronze.getSourceFile(),
            bronze.getReportDate(),
            bronze.getStateRaw(),
            bronze.getRequirementRaw(),
            bronze.getSupplyRaw(),
            bronze.getShortageRaw(),
            bronze.getShortagePercentageRaw(),
            bronze.getAdditionalFields(),
            bronze.getFileChecksum(),
            bronze.getIngestionBatchId()
        );
    }

    /**
     * Batch save - more efficient for multiple records
     */
    public int[][] saveBatch(List<BronzePowerData> bronzeRecords) {
        String sql = """
            INSERT INTO bronze_power_data (
                data_source, source_file, report_date, state_raw, requirement_raw,
                supply_raw, shortage_raw, shortage_percentage_raw, additional_fields,
                file_checksum, ingestion_batch_id, ingestion_timestamp
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        return jdbcTemplate.batchUpdate(sql, bronzeRecords, bronzeRecords.size(),
            (ps, bronze) -> {
                ps.setString(1, bronze.getDataSource());
                ps.setString(2, bronze.getSourceFile());
                ps.setDate(3, java.sql.Date.valueOf(bronze.getReportDate()));
                ps.setString(4, bronze.getStateRaw());
                ps.setString(5, bronze.getRequirementRaw());
                ps.setString(6, bronze.getSupplyRaw());
                ps.setString(7, bronze.getShortageRaw());
                ps.setString(8, bronze.getShortagePercentageRaw());
                ps.setString(9, bronze.getAdditionalFields());
                ps.setString(10, bronze.getFileChecksum());
                ps.setString(11, bronze.getIngestionBatchId());
            }
        );
    }

    /**
     * Delete old records (cleanup)
     */
    public int deleteOlderThan(LocalDate date) {
        String sql = "DELETE FROM bronze_power_data WHERE report_date < ?";
        return jdbcTemplate.update(sql, date);
    }

    // Helper class for ingestion statistics
    public record IngestionStats(
        String dataSource,
        Long recordCount,
        Long uniqueFiles,
        LocalDate earliestDate,
        LocalDate latestDate
    ) {}
}
