package com.power.pipeline.bronze.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bronze_power_data", indexes = {
    @Index(name = "idx_bronze_source_date", columnList = "data_source,report_date"),
    @Index(name = "idx_bronze_ingestion", columnList = "ingestion_timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BronzePowerData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource;

    @Column(name = "source_file", length = 255)
    private String sourceFile;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "state_raw", length = 100)
    private String stateRaw;

    @Column(name = "requirement_raw", length = 50)
    private String requirementRaw;

    @Column(name = "supply_raw", length = 50)
    private String supplyRaw;

    @Column(name = "shortage_raw", length = 50)
    private String shortageRaw;

    @Column(name = "shortage_percentage_raw", length = 50)
    private String shortagePercentageRaw;

    @Column(name = "additional_fields", columnDefinition = "TEXT")
    private String additionalFields;

    @Column(name = "ingestion_timestamp", nullable = false)
    private LocalDateTime ingestionTimestamp;

    @Column(name = "file_checksum", length = 64)
    private String fileChecksum;

    @Column(name = "ingestion_batch_id", length = 50)
    private String ingestionBatchId;

    @PrePersist
    protected void onCreate() {
        if (ingestionTimestamp == null) {
            ingestionTimestamp = LocalDateTime.now();
        }
    }
}
