package com.power.pipeline.silver.repository;

import com.power.pipeline.silver.entity.SilverPowerData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SilverPowerDataJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    // RowMapper - converts SQL result to Java object
    private final RowMapper<SilverPowerData> rowMapper = (rs, rowNum) ->
        SilverPowerData.builder()
            .id(rs.getLong("id"))
            .dataSource(rs.getString("data_source"))
            .state(rs.getString("state"))
            .reportDate(rs.getDate("report_date").toLocalDate())
            .energyRequiredMu(getDoubleOrNull(rs, "energy_required_mu"))
            .energySuppliedMu(getDoubleOrNull(rs, "energy_supplied_mu"))
            .energyShortfallMu(getDoubleOrNull(rs, "energy_shortfall_mu"))
            .shortfallPercentage(getDoubleOrNull(rs, "shortfall_percentage"))
            .sourcePriority(rs.getInt("source_priority"))
            .qualityScore(rs.getDouble("quality_score"))
            .qualityFlags(rs.getString("quality_flags"))
            .isValid(rs.getBoolean("is_valid"))
            .bronzeRecordId(rs.getLong("bronze_record_id"))
            .build();

    /**
     * Find all records
     */
    public List<SilverPowerData> findAll() {
        String sql = "SELECT * FROM silver_power_data ORDER BY report_date DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Find by state and date
     */
    public List<SilverPowerData> findByStateAndReportDate(String state, LocalDate reportDate) {
        String sql = "SELECT * FROM silver_power_data WHERE state = ? AND report_date = ?";
        return jdbcTemplate.query(sql, rowMapper, state, reportDate);
    }

    /**
     * Find valid records sorted by priority and quality (for Gold aggregation)
     */
    public List<SilverPowerData> findValidRecordsForGoldAggregation(
            String state, LocalDate reportDate) {

        String sql = """
            SELECT * FROM silver_power_data
            WHERE state = ?
              AND report_date = ?
              AND is_valid = true
            ORDER BY source_priority ASC, quality_score DESC
            """;

        return jdbcTemplate.query(sql, rowMapper, state, reportDate);
    }

    /**
     * Find by date range and validity
     */
    public List<SilverPowerData> findByDateRangeAndValidity(
            LocalDate startDate, LocalDate endDate, Boolean isValid) {

        String sql = """
            SELECT * FROM silver_power_data
            WHERE report_date BETWEEN ? AND ?
              AND is_valid = ?
            ORDER BY report_date, state
            """;

        return jdbcTemplate.query(sql, rowMapper, startDate, endDate, isValid);
    }

    /**
     * Get all distinct states
     */
    public List<String> findDistinctStates() {
        String sql = "SELECT DISTINCT state FROM silver_power_data ORDER BY state";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Count records
     */
    public Long count() {
        String sql = "SELECT COUNT(*) FROM silver_power_data";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * Count by validity
     */
    public Long countByValidity(Boolean isValid) {
        String sql = "SELECT COUNT(*) FROM silver_power_data WHERE is_valid = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, isValid);
    }

    /**
     * Find records with quality issues
     */
    public List<SilverPowerData> findRecordsWithQualityIssues() {
        String sql = """
            SELECT * FROM silver_power_data
            WHERE quality_flags IS NOT NULL
               OR quality_score < 0.8
            ORDER BY quality_score ASC
            """;

        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Get quality statistics by data source
     */
    public List<SourceQualityStats> getQualityStatsBySource() {
        String sql = """
            SELECT data_source,
                   COUNT(*) as total_records,
                   AVG(quality_score) as avg_quality,
                   MIN(quality_score) as min_quality,
                   MAX(quality_score) as max_quality,
                   SUM(CASE WHEN is_valid = true THEN 1 ELSE 0 END) as valid_count,
                   SUM(CASE WHEN is_valid = false THEN 1 ELSE 0 END) as invalid_count
            FROM silver_power_data
            GROUP BY data_source
            ORDER BY avg_quality DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SourceQualityStats(
            rs.getString("data_source"),
            rs.getLong("total_records"),
            rs.getDouble("avg_quality"),
            rs.getDouble("min_quality"),
            rs.getDouble("max_quality"),
            rs.getLong("valid_count"),
            rs.getLong("invalid_count")
        ));
    }

    /**
     * Save a record
     */
    public int save(SilverPowerData silver) {
        String sql = """
            INSERT INTO silver_power_data (
                data_source, state, report_date, energy_required_mu, energy_supplied_mu,
                energy_shortfall_mu, shortfall_percentage, source_priority,
                quality_score, quality_flags, is_valid, bronze_record_id, ingestion_timestamp
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        return jdbcTemplate.update(sql,
            silver.getDataSource(),
            silver.getState(),
            silver.getReportDate(),
            silver.getEnergyRequiredMu(),
            silver.getEnergySuppliedMu(),
            silver.getEnergyShortfallMu(),
            silver.getShortfallPercentage(),
            silver.getSourcePriority(),
            silver.getQualityScore(),
            silver.getQualityFlags(),
            silver.getIsValid(),
            silver.getBronzeRecordId()
        );
    }

    // Helper method to handle NULL doubles
    private Double getDoubleOrNull(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    // Helper class for quality statistics
    public record SourceQualityStats(
        String dataSource,
        Long totalRecords,
        Double avgQuality,
        Double minQuality,
        Double maxQuality,
        Long validCount,
        Long invalidCount
    ) {}
}
