#!/bin/bash

# Demo Scenarios for Power Data Pipeline
# This script demonstrates key capabilities: idempotency and corrections handling

echo "=========================================="
echo "Power Data Pipeline - Demo Scenarios"
echo "=========================================="
echo ""

# Function to check if server is up
wait_for_server() {
    echo "Waiting for server to start..."
    for i in {1..30}; do
        if curl -s http://localhost:8080/api/pipeline/stats > /dev/null 2>&1; then
            echo "Server is up!"
            return 0
        fi
        sleep 2
    done
    echo "Server did not start in time"
    return 1
}

# Scenario 1: Initial Pipeline Run
echo "=========================================="
echo "Scenario 1: Initial Pipeline Run"
echo "=========================================="
echo "This demonstrates the first-time ingestion of data through all three layers."
echo ""

read -p "Press Enter to run the pipeline for the first time..."

echo "Running pipeline..."
curl -X POST http://localhost:8080/api/pipeline/run | python -m json.tool
echo ""

echo "Fetching pipeline stats..."
curl -s http://localhost:8080/api/pipeline/stats | python -m json.tool
echo ""

echo "Fetching Maharashtra data..."
curl -s "http://localhost:8080/api/pipeline/data?state=Maharashtra" | python -m json.tool | head -50
echo ""

# Scenario 2: Pipeline Idempotency (Rerun without changes)
echo "=========================================="
echo "Scenario 2: Pipeline Idempotency"
echo "=========================================="
echo "This demonstrates that re-running the pipeline with the same files"
echo "does NOT create duplicate records (idempotency via checksum)."
echo ""

read -p "Press Enter to re-run the pipeline with identical files..."

echo "Running pipeline again (should skip bronze ingestion)..."
curl -X POST http://localhost:8080/api/pipeline/run | python -m json.tool
echo ""

echo "Notice: Bronze records = 0 (already ingested, checksum match)"
echo "Silver and Gold still process to ensure consistency"
echo ""

# Scenario 3: Processing a Correction
echo "=========================================="
echo "Scenario 3: Processing a Data Correction"
echo "=========================================="
echo "This demonstrates handling corrected data from a source."
echo "We'll simulate POSOCO publishing a correction for Nov 15 data."
echo ""

read -p "Press Enter to create a corrected data file..."

# Create a correction file (modify one record)
cat > data/raw/posoco_correction_2026-11-15.csv << 'EOF'
Date,State,Requirement_MU,Availability_MU,Shortage_MU,Shortage_Percentage
2026-11-15,Maharashtra,312.4,310.5,1.9,0.61
2026-11-15,Gujarat,201.3,200.5,0.8,0.40
2026-11-15,Goa,8.7,8.7,0.0,0.00
EOF

echo "Created correction file: posoco_correction_2026-11-15.csv"
echo "Changes:"
echo "  - Maharashtra: Shortage reduced from 3.5 MU to 1.9 MU"
echo "  - Gujarat: Shortage reduced from 1.6 MU to 0.8 MU"
echo ""

read -p "Press Enter to process the correction..."

echo "Running pipeline with correction..."
curl -X POST http://localhost:8080/api/pipeline/run | python -m json.tool
echo ""

echo "Fetching updated Nov 15 data for Maharashtra..."
curl -s "http://localhost:8080/api/pipeline/data?state=Maharashtra" | \
    python -m json.tool | \
    grep -A 10 "2026-11-15"
echo ""

echo "The gold layer now reflects the corrected values!"
echo ""

# Scenario 4: Quality Issues Detection
echo "=========================================="
echo "Scenario 4: Data Quality Issues"
echo "=========================================="
echo "This demonstrates the pipeline's ability to detect and report"
echo "source disagreements and quality issues."
echo ""

read -p "Press Enter to view quality issues..."

echo "Records with source disagreement (< 95% agreement):"
curl -s "http://localhost:8080/api/pipeline/data/quality-issues?agreementThreshold=95.0" | \
    python -m json.tool
echo ""

# Scenario 5: Late Arrival Handling
echo "=========================================="
echo "Scenario 5: Late File Arrival"
echo "=========================================="
echo "This demonstrates handling when SLDC data arrives after POSOCO data."
echo "We'll add a 'late' SLDC file for Gujarat."
echo ""

read -p "Press Enter to simulate late file arrival..."

# Create a late file
cat > data/raw/sldc_gujarat_late_2026-11-18.csv << 'EOF'
Date,Peak_Demand_MW,Average_Demand_MW,Energy_Supplied_MU,Energy_Required_MU,Shortage_MU
2026-11-18,8890,8321,198.50,199.70,1.20
EOF

echo "Created late SLDC Gujarat file for Nov 18"
echo "This provides a second source for validation."
echo ""

read -p "Press Enter to process the late file..."

echo "Running pipeline..."
curl -X POST http://localhost:8080/api/pipeline/run | python -m json.tool
echo ""

echo "Nov 18 Gujarat data now has multiple sources for comparison!"
echo ""

# Summary
echo "=========================================="
echo "Demo Complete!"
echo "=========================================="
echo ""
echo "You have seen:"
echo "  ✓ Initial pipeline execution (Bronze → Silver → Gold)"
echo "  ✓ Idempotency (reruns don't duplicate data)"
echo "  ✓ Correction handling (updated values flow through)"
echo "  ✓ Quality issue detection (source disagreements flagged)"
echo "  ✓ Late file handling (additional sources integrated)"
echo ""
echo "Key capabilities demonstrated:"
echo "  • Checksum-based duplicate prevention"
echo "  • Multi-source reconciliation"
echo "  • Data quality scoring"
echo "  • Audit trail (Bronze preserves all versions)"
echo "  • API-based access to curated data"
echo ""
echo "For more details, see:"
echo "  - docs/data-sources.md"
echo "  - docs/data-model-design.md"
echo "  - docs/analytical-examples.md"
echo ""
