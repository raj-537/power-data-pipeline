package com.power.pipeline.gold.repository;

import com.power.pipeline.gold.entity.GoldPowerData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GoldPowerDataJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    // RowMapper - converts SQL result to Java object
    private final RowMapper<GoldPowerData> rowMapper = new RowMapper<GoldPowerData>() {
        @Override
        public GoldPowerData mapRow(ResultSet rs, int rowNum) throws SQLException {
            return GoldPowerData.builder()
                    .id(rs.getLong("id"))
                    .state(rs.getString("state"))
                    .reportDate(rs.getDate("report_date").toLocalDate())
                    .energyRequiredMu(rs.getDouble("energy_required_mu"))
                    .energySuppliedMu(rs.getDouble("energy_supplied_mu"))
                    .energyShortfallMu(rs.getDouble("energy_shortfall_mu"))
                    .shortfallPercentage(rs.getDouble("shortfall_percentage"))
                    .primarySource(rs.getString("primary_source"))
                    .secondarySource(rs.getString("secondary_source"))
                    .sourceAgreementPercentage(
                        rs.getObject("source_agreement_percentage") != null ?
                        rs.getDouble("source_agreement_percentage") : null
                    )
                    .dataQualityScore(rs.getDouble("data_quality_score"))
                    .confidenceLevel(rs.getString("confidence_level"))
                    .qualityNotes(rs.getString("quality_notes"))
                    .silverRecordIds(rs.getString("silver_record_ids"))
                    .build();
        }
    };

    /**
     * Find all records
     */
    public List<GoldPowerData> findAll() {
        String sql = "SELECT * FROM gold_power_data ORDER BY report_date DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Find by state and date
     */
    public Optional<GoldPowerData> findByStateAndReportDate(String state, LocalDate reportDate) {
        String sql = "SELECT * FROM gold_power_data WHERE state = ? AND report_date = ?";

        List<GoldPowerData> results = jdbcTemplate.query(sql, rowMapper, state, reportDate);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find by state, sorted by date descending
     */
    public List<GoldPowerData> findByState(String state) {
        String sql = "SELECT * FROM gold_power_data WHERE state = ? ORDER BY report_date DESC";
        return jdbcTemplate.query(sql, rowMapper, state);
    }

    /**
     * Find by confidence level
     */
    public List<GoldPowerData> findByConfidenceLevel(String confidenceLevel) {
        String sql = "SELECT * FROM gold_power_data WHERE confidence_level = ?";
        return jdbcTemplate.query(sql, rowMapper, confidenceLevel);
    }

    /**
     * Find records with source disagreement
     */
    public List<GoldPowerData> findRecordsWithSourceDisagreement(Double threshold) {
        String sql = """
            SELECT * FROM gold_power_data
            WHERE secondary_source IS NOT NULL
              AND source_agreement_percentage < ?
            ORDER BY source_agreement_percentage ASC
            """;

        return jdbcTemplate.query(sql, rowMapper, threshold);
    }

    /**
     * Find by date range
     */
    public List<GoldPowerData> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM gold_power_data
            WHERE report_date BETWEEN ? AND ?
            ORDER BY report_date, state
            """;

        return jdbcTemplate.query(sql, rowMapper, startDate, endDate);
    }

    /**
     * Find by multiple states and date range
     */
    public List<GoldPowerData> findByStatesAndDateRange(
            List<String> states, LocalDate startDate, LocalDate endDate) {

        // Build IN clause: state IN (?, ?, ?)
        String placeholders = String.join(",", states.stream().map(s -> "?").toList());

        String sql = String.format("""
            SELECT * FROM gold_power_data
            WHERE state IN (%s)
              AND report_date BETWEEN ? AND ?
            ORDER BY state, report_date
            """, placeholders);

        // Build parameters: states + startDate + endDate
        Object[] params = new Object[states.size() + 2];
        for (int i = 0; i < states.size(); i++) {
            params[i] = states.get(i);
        }
        params[states.size()] = startDate;
        params[states.size() + 1] = endDate;

        return jdbcTemplate.query(sql, rowMapper, params);
    }

    /**
     * Get average shortage by state (returns raw data)
     */
    public List<StateShortageStats> getAverageShortageByState() {
        String sql = """
            SELECT state,
                   AVG(energy_shortfall_mu) as avg_shortage,
                   MAX(energy_shortfall_mu) as max_shortage,
                   MIN(energy_shortfall_mu) as min_shortage,
                   COUNT(*) as record_count
            FROM gold_power_data
            GROUP BY state
            ORDER BY avg_shortage DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StateShortageStats(
            rs.getString("state"),
            rs.getDouble("avg_shortage"),
            rs.getDouble("max_shortage"),
            rs.getDouble("min_shortage"),
            rs.getInt("record_count")
        ));
    }

    /**
     * Count records
     */
    public Long count() {
        String sql = "SELECT COUNT(*) FROM gold_power_data";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * Count by confidence level
     */
    public Long countByConfidenceLevel(String confidenceLevel) {
        String sql = "SELECT COUNT(*) FROM gold_power_data WHERE confidence_level = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, confidenceLevel);
    }

    /**
     * Save a record (INSERT or UPDATE)
     */
    public int save(GoldPowerData gold) {
        // Check if exists
        Optional<GoldPowerData> existing = findByStateAndReportDate(
            gold.getState(), gold.getReportDate()
        );

        if (existing.isPresent()) {
            // UPDATE
            String sql = """
                UPDATE gold_power_data SET
                    energy_required_mu = ?,
                    energy_supplied_mu = ?,
                    energy_shortfall_mu = ?,
                    shortfall_percentage = ?,
                    primary_source = ?,
                    secondary_source = ?,
                    source_agreement_percentage = ?,
                    data_quality_score = ?,
                    confidence_level = ?,
                    quality_notes = ?,
                    silver_record_ids = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE state = ? AND report_date = ?
                """;

            return jdbcTemplate.update(sql,
                gold.getEnergyRequiredMu(),
                gold.getEnergySuppliedMu(),
                gold.getEnergyShortfallMu(),
                gold.getShortfallPercentage(),
                gold.getPrimarySource(),
                gold.getSecondarySource(),
                gold.getSourceAgreementPercentage(),
                gold.getDataQualityScore(),
                gold.getConfidenceLevel(),
                gold.getQualityNotes(),
                gold.getSilverRecordIds(),
                gold.getState(),
                gold.getReportDate()
            );
        } else {
            // INSERT
            String sql = """
                INSERT INTO gold_power_data (
                    state, report_date, energy_required_mu, energy_supplied_mu,
                    energy_shortfall_mu, shortfall_percentage, primary_source,
                    secondary_source, source_agreement_percentage, data_quality_score,
                    confidence_level, quality_notes, silver_record_ids,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

            return jdbcTemplate.update(sql,
                gold.getState(),
                gold.getReportDate(),
                gold.getEnergyRequiredMu(),
                gold.getEnergySuppliedMu(),
                gold.getEnergyShortfallMu(),
                gold.getShortfallPercentage(),
                gold.getPrimarySource(),
                gold.getSecondarySource(),
                gold.getSourceAgreementPercentage(),
                gold.getDataQualityScore(),
                gold.getConfidenceLevel(),
                gold.getQualityNotes(),
                gold.getSilverRecordIds()
            );
        }
    }

    /**
     * Delete by state and date
     */
    public int delete(String state, LocalDate reportDate) {
        String sql = "DELETE FROM gold_power_data WHERE state = ? AND report_date = ?";
        return jdbcTemplate.update(sql, state, reportDate);
    }

    /**
     * Custom complex query example
     */
    public List<GoldPowerData> findQualityIssues() {
        String sql = """
            SELECT * FROM gold_power_data
            WHERE data_quality_score < 0.8
               OR confidence_level = 'LOW'
               OR (secondary_source IS NOT NULL
                   AND source_agreement_percentage < 90.0)
            ORDER BY data_quality_score ASC
            """;

        return jdbcTemplate.query(sql, rowMapper);
    }

    // Helper class for aggregation results
    public record StateShortageStats(
        String state,
        Double avgShortage,
        Double maxShortage,
        Double minShortage,
        Integer recordCount
    ) {}
}
