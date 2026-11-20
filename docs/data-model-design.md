# Data Model Design

## Three-Layer Architecture

**Bronze** → Raw data preservation  
**Silver** → Cleaned & validated  
**Gold** → Analysis-ready (one record per state-date)

---

## Bronze Layer (Raw)

Stores raw CSV data as strings with metadata.

**Key Columns**: `data_source`, `source_file`, `report_date`, `state_raw`, `requirement_raw`, `supply_raw`, `shortage_raw`, `file_checksum`, `ingestion_batch_id`

**Idempotency**: SHA-256 file checksum prevents duplicate ingestion

---

## Silver Layer (Cleaned)

Parsed, validated data with quality scoring.

**Key Columns**: `state` (standardized), `energy_required_mu`, `energy_supplied_mu`, `energy_shortfall_mu`, `source_priority`, `bronze_record_id`, `quality_score`, `quality_flags`, `is_valid`

**Quality Checks**:
- Missing values → -0.2
- Math inconsistency → -0.3
- Negative values → -0.2

**Idempotency**: Tracks `bronze_record_id` to prevent reprocessing

---

## Gold Layer (Final)

One authoritative record per (state, date) with conflict resolution.

**Key Columns**: `state`, `report_date`, `energy_*_mu`, `primary_source`, `secondary_source`, `source_agreement_percentage`, `data_quality_score`, `confidence_level`

**Conflict Resolution**: 
1. Sort by source priority (POSOCO=1, SLDC=2)
2. Sort by quality score
3. Pick best, track alternatives

**Confidence Levels**:
- `HIGH`: Quality ≥ 0.9, sources agree
- `MEDIUM`: Quality ≥ 0.7 or single source
- `LOW`: Quality < 0.7 or disagreement

---

## Data Flow

```
CSV Files → Bronze (raw strings + checksum)
         → Silver (parsed + validated + quality score)
         → Gold (best record per state-date + confidence)
```

---

## Key Design Choices

1. **String storage in Bronze** - Preserves raw data, enables reprocessing
2. **Quality metadata** - Transparency for data limitations
3. **Source priority** - POSOCO > SLDC (configurable)
4. **Update-or-insert in Gold** - Reruns don't create duplicates
5. **Idempotency at all layers** - Safe to rerun pipeline

---

## Quick Reference

**Find quality issues:**
```sql
SELECT * FROM gold_power_data 
WHERE confidence_level = 'MEDIUM' 
   OR source_agreement_percentage < 95.0;
```

**Audit trail:**
```sql
-- Gold → Silver → Bronze
SELECT g.*, s.*, b.*
FROM gold_power_data g
JOIN silver_power_data s ON s.id = ANY(string_to_array(g.silver_record_ids, ',')::bigint[])
JOIN bronze_power_data b ON b.id = s.bronze_record_id
WHERE g.state = 'Maharashtra' AND g.report_date = '2026-11-15';
```
