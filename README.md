# Power Data Pipeline

A reliable state-level electricity data pipeline for Indian states, built with Spring Boot.

## Overview

This project builds a trustworthy daily state-level dataset of electricity required, supplied, and shortfall for Indian states using publicly available data sources.

## Architecture

### Three-Layer Design

1. **Bronze Layer**: Raw data ingestion with minimal transformation
2. **Silver Layer**: Cleaned, validated, and standardized data
3. **Gold Layer**: Analysis-ready aggregated data with conflict resolution

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **H2 Database** (embedded)
- **Flyway** (database migrations)
- **Maven**
- **AWS Bedrock** (optional - for LLM mode)

## Project Structure

```
power-data-pipeline/
├── src/
│   ├── main/
│   │   ├── java/com/power/pipeline/
│   │   │   ├── bronze/      # Raw data ingestion
│   │   │   ├── silver/      # Data cleaning & validation
│   │   │   ├── gold/        # Analysis-ready data
│   │   │   ├── common/      # Shared utilities
│   │   │   └── api/         # REST API controllers
│   │   └── resources/
│   │       ├── db/migration/    # Flyway scripts
│   │       └── sample-data/     # Downloaded data samples
│   └── test/
├── data/
│   ├── raw/         # Original downloaded files
│   ├── bronze/      # Bronze layer data
│   ├── silver/      # Silver layer data
│   └── gold/        # Gold layer data
└── docs/            # Documentation

```

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven 3.8+

### Build and Run

```bash
# Build the project
mvn clean install

# Option 1: Run with rule-based mode only (recommended for demo)
start.bat

# Option 2: Run with AWS Bedrock LLM enabled
start-with-llm.bat

# Option 3: Manual start (rule-based only)
mvn spring-boot:run

# Access H2 Console
# URL: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:file:./data/powerdb
```

**Note:** `start-with-llm.bat` includes AWS credentials and enables LLM mode. Use `start.bat` for standard operation.

## Data Sources

The pipeline uses two primary sources:

1. **POSOCO (Power System Operation Corporation)** - National Load Dispatch Centre
   - Official national authority for power system data
   - Daily energy reports covering all Indian states
   - Priority level: 1 (highest)

2. **State Load Dispatch Centers (SLDCs)** - State-level utilities
   - Maharashtra SLDC, Gujarat SLDC, Goa Electricity Department
   - State-specific granular data
   - Priority level: 2

See [docs/data-sources.md](docs/data-sources.md) for detailed information.

## Design Decisions

### Three-Layer Architecture (Medallion)

**Bronze Layer**: Raw data storage
- Preserves original data exactly as received
- All fields stored as strings
- Includes full metadata (source, timestamps, checksums)
- Enables audit trail and reprocessing

**Silver Layer**: Cleaned and validated data
- Parsed and standardized values
- Data quality checks applied  
- Quality scores and flags attached
- Multiple versions allowed per state-date

**Gold Layer**: Analysis-ready data
- One authoritative record per state-date
- Source conflicts resolved
- Confidence levels assigned
- Optimized for queries

### Key Features

✅ **Idempotency**: File checksums prevent duplicate ingestion  
✅ **Data Quality**: Automated validation and scoring  
✅ **Multi-source**: Reconciles data from multiple sources  
✅ **Auditable**: Complete lineage from raw to gold  
✅ **API-first**: REST API for data access  
✅ **Q&A Agent**: Natural language queries with data quality context  

See [docs/data-model-design.md](docs/data-model-design.md) for architecture details.

## Usage

### Running the Pipeline

**Method 1: API Endpoint**
```bash
# Start the application
mvn spring-boot:run

# Trigger pipeline execution
curl -X POST http://localhost:8080/api/pipeline/run
```

**Method 2: Test Script**
```bash
chmod +x test-pipeline.sh
./test-pipeline.sh
```

### API Endpoints

**Run Pipeline**
```bash
POST /api/pipeline/run
```

**Get Data**
```bash
# All data
GET /api/pipeline/data

# By state
GET /api/pipeline/data?state=Maharashtra

# By date range
GET /api/pipeline/data?startDate=2026-11-13&endDate=2026-11-19
```

**Get Statistics**
```bash
GET /api/pipeline/stats
```

**Get Quality Issues**
```bash
GET /api/pipeline/data/quality-issues?agreementThreshold=95.0
```

**Q&A Agent** 🤖
```bash
# Rule-based mode (default, fast)
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Which state had the highest shortage?"}'

# LLM mode (optional, requires AWS Bedrock setup)
curl -X POST "http://localhost:8080/api/qa/ask?mode=llm" \
  -H "Content-Type: application/json" \
  -d '{"question": "Analyze power shortage patterns"}'

# See available commands
GET /api/qa/help
```

**Note:** Rule-based mode is production-ready and works out-of-the-box. LLM mode requires AWS Bedrock setup (see [AWS_BEDROCK_SETUP.md](AWS_BEDROCK_SETUP.md)).

### Demo Scenarios

Run the interactive demo to see:
- Initial pipeline execution
- Idempotency (reruns without duplicates)
- Correction handling
- Late file arrival

```bash
chmod +x demo-scenarios.sh
./demo-scenarios.sh
```

### Example Results

See [docs/analytical-examples.md](docs/analytical-examples.md) for:
- Sample queries and analyses
- Data quality metrics
- Visualization ideas
- Limitation documentation

## License

MIT
