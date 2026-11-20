package com.power.pipeline.silver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "silver_power_data",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_silver_source_state_date",
        columnNames = {"data_source", "state", "report_date", "ingestion_timestamp"}
    ),
    indexes = {
        @Index(name = "idx_silver_state_date", columnList = "state,report_date"),
        @Index(name = "idx_silver_source", columnList = "data_source")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SilverPowerData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "energy_required_mu")
    private Double energyRequiredMu;

    @Column(name = "energy_supplied_mu")
    private Double energySuppliedMu;

    @Column(name = "energy_shortfall_mu")
    private Double energyShortfallMu;

    @Column(name = "shortfall_percentage")
    private Double shortfallPercentage;

    @Column(name = "source_priority", nullable = false)
    private Integer sourcePriority;

    @Column(name = "ingestion_timestamp", nullable = false)
    private LocalDateTime ingestionTimestamp;

    @Column(name = "bronze_record_id")
    private Long bronzeRecordId;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "quality_flags", length = 500)
    private String qualityFlags;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid;

    @PrePersist
    protected void onCreate() {
        if (ingestionTimestamp == null) {
            ingestionTimestamp = LocalDateTime.now();
        }
        if (isValid == null) {
            isValid = true;
        }
    }
}
