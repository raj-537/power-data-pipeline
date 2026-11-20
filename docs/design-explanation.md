# Design Explanation

## Architecture

```
CSV Files → Bronze (raw + checksum)
         → Silver (cleaned + quality score)
         → Gold (best record per state-date)
         → REST API
```

---

## Key Design Decisions

### 1. Three-Layer Medallion Architecture

**Why:** Separates concerns - raw preservation (bronze), validation (silver), analysis (gold)

**Trade-off:** More storage vs. flexibility to reprocess

### 2. Idempotency at Every Layer

**Bronze:** File checksum (SHA-256)  
**Silver:** Tracks `bronze_record_id`  
**Gold:** Update-or-insert on `(state, date)`

**Why:** Safe to rerun pipeline multiple times without duplicates

### 3. Quality-First Approach

Every silver record gets:
- Quality score (0.0-1.0)
- Quality flags (MISSING_VALUES, MATH_INCONSISTENCY, etc.)
- Validation status

**Why:** Users need to know data limitations upfront

### 4. Source Priority System

POSOCO (priority=1) > SLDC (priority=2)

**Why:** POSOCO is national authority, but we track alternatives for transparency

### 5. State Name Extraction from Filenames

SLDC files don't have "State" column → extract from filename

Example: `sldc_maharashtra_*.csv` → "Maharashtra"

**Why:** Real-world data isn't always clean

---

## Component Responsibilities

**BronzeIngestionService**
- Parse CSV files
- Calculate checksums
- Store raw strings

**SilverProcessingService**  
- Parse numbers
- Standardize state names
- Calculate quality scores
- Validate math consistency

**GoldAggregationService**
- Sort by priority + quality
- Pick best record
- Calculate source agreement
- Determine confidence level

**QAController** (Optional Feature)
- Rule-based Q&A (keyword matching)
- AWS Bedrock LLM integration (optional)
- Always includes quality context

---

## Performance

**Current:** 42 records in ~390ms

**Bottlenecks at Scale:**
- Bronze: CSV parsing (CPU-bound) → parallel processing
- Silver: DB I/O → batch inserts
- Gold: Joins → materialized views

**Conclusion:** Current design handles years of data for 28 states

---

## Error Handling

**Bronze fails:** Stop pipeline (data loss risk)  
**Silver fails:** Log + continue (partial processing OK)  
**Gold fails:** Skip state-date + continue

**Why:** Bronze is critical, silver/gold are recoverable

---

## Assumptions

1. CSV format stays stable (mitigation: bronze preserves raw)
2. POSOCO always more reliable (mitigation: configurable priority)
3. Daily granularity sufficient (mitigation: architecture supports hourly)
4. State names can be standardized (mitigation: extensible parser)

---

## Trade-offs

| Choice | Benefit | Cost |
|--------|---------|------|
| H2 Database | Easy setup | Not production-scale |
| Sync processing | Simple | Not high-volume ready |
| String bronze | Raw preservation | Parse overhead |
| Quality metadata | Transparency | Storage cost |

---

## Future Enhancements

**Short-term:**
- Automated scraping (web + schedule)
- Enhanced quality checks (outliers)

**Medium-term:**
- All 28 states coverage
- Predictive models

**Long-term:**
- Real-time streaming (Kafka)
- ML anomaly detection

---

## Interview Talking Points

✅ **Medallion architecture** - industry standard (Databricks)  
✅ **Idempotency** - production requirement  
✅ **Quality transparency** - users need context  
✅ **Full lineage** - audit trail from raw to gold  
✅ **RESTful API** - downstream integration  
✅ **Optional LLM** - shows cloud AI skills (AWS Bedrock)

Time: ~7 hours | LOC: ~1,500 | Status: Production-ready for interview demo
