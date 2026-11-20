package com.power.pipeline.gold.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gold_power_data",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_gold_state_date",
        columnNames = {"state", "report_date"}
    ),
    indexes = {
        @Index(name = "idx_gold_state", columnList = "state"),
        @Index(name = "idx_gold_date", columnList = "report_date"),
        @Index(name = "idx_gold_state_date", columnList = "state,report_date")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldPowerData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "energy_required_mu", nullable = false)
    private Double energyRequiredMu;

    @Column(name = "energy_supplied_mu", nullable = false)
    private Double energySuppliedMu;

    @Column(name = "energy_shortfall_mu", nullable = false)
    private Double energyShortfallMu;

    @Column(name = "shortfall_percentage", nullable = false)
    private Double shortfallPercentage;

    @Column(name = "primary_source", nullable = false, length = 50)
    private String primarySource;

    @Column(name = "secondary_source", length = 50)
    private String secondarySource;

    @Column(name = "source_agreement_percentage")
    private Double sourceAgreementPercentage;

    @Column(name = "data_quality_score")
    private Double dataQualityScore;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel;

    @Column(name = "quality_notes", columnDefinition = "TEXT")
    private String qualityNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "silver_record_ids", length = 500)
    private String silverRecordIds;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
