-- Bronze Layer: Raw ingested data
CREATE TABLE bronze_power_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_source VARCHAR(50) NOT NULL,
    source_file VARCHAR(255),
    report_date DATE NOT NULL,
    state_raw VARCHAR(100),
    requirement_raw VARCHAR(50),
    supply_raw VARCHAR(50),
    shortage_raw VARCHAR(50),
    shortage_percentage_raw VARCHAR(50),
    additional_fields TEXT,
    ingestion_timestamp TIMESTAMP NOT NULL,
    file_checksum VARCHAR(64),
    ingestion_batch_id VARCHAR(50)
);

CREATE INDEX idx_bronze_source_date ON bronze_power_data(data_source, report_date);
CREATE INDEX idx_bronze_ingestion ON bronze_power_data(ingestion_timestamp);

-- Silver Layer: Cleaned and validated data
CREATE TABLE silver_power_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_source VARCHAR(50) NOT NULL,
    state VARCHAR(100) NOT NULL,
    report_date DATE NOT NULL,
    energy_required_mu DOUBLE,
    energy_supplied_mu DOUBLE,
    energy_shortfall_mu DOUBLE,
    shortfall_percentage DOUBLE,
    source_priority INT NOT NULL,
    ingestion_timestamp TIMESTAMP NOT NULL,
    bronze_record_id BIGINT,
    quality_score DOUBLE,
    quality_flags VARCHAR(500),
    validation_errors TEXT,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_silver_source_state_date UNIQUE (data_source, state, report_date, ingestion_timestamp)
);

CREATE INDEX idx_silver_state_date ON silver_power_data(state, report_date);
CREATE INDEX idx_silver_source ON silver_power_data(data_source);

-- Gold Layer: Analysis-ready aggregated data
CREATE TABLE gold_power_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    state VARCHAR(100) NOT NULL,
    report_date DATE NOT NULL,
    energy_required_mu DOUBLE NOT NULL,
    energy_supplied_mu DOUBLE NOT NULL,
    energy_shortfall_mu DOUBLE NOT NULL,
    shortfall_percentage DOUBLE NOT NULL,
    primary_source VARCHAR(50) NOT NULL,
    secondary_source VARCHAR(50),
    source_agreement_percentage DOUBLE,
    data_quality_score DOUBLE,
    confidence_level VARCHAR(20),
    quality_notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    silver_record_ids VARCHAR(500),
    CONSTRAINT uk_gold_state_date UNIQUE (state, report_date)
);

CREATE INDEX idx_gold_state ON gold_power_data(state);
CREATE INDEX idx_gold_date ON gold_power_data(report_date);
CREATE INDEX idx_gold_state_date ON gold_power_data(state, report_date);
