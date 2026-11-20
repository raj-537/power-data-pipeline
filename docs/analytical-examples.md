# Analytical Examples

## Sample Queries

### 1. State-wise Shortage Summary

```sql
SELECT state, AVG(energy_shortfall_mu) as avg_shortfall,
       AVG(shortfall_percentage) as avg_shortfall_pct
FROM gold_power_data
GROUP BY state ORDER BY avg_shortfall DESC;
```

**Result:** Maharashtra (2.79 MU) > Gujarat (1.06 MU) > Goa (0.04 MU)

**API:** `GET /api/pipeline/data`

---

### 2. Quality Assessment

```sql
SELECT confidence_level, COUNT(*) as count,
       AVG(data_quality_score) as avg_quality
FROM gold_power_data
GROUP BY confidence_level;
```

**Result:**
- HIGH: 14 records (66.7%)
- MEDIUM: 7 records (33.3%)
- Average quality: 0.97/1.00

**API:** `GET /api/pipeline/stats`

---

### 3. Source Disagreements

```sql
SELECT state, report_date, primary_source, secondary_source,
       source_agreement_percentage
FROM gold_power_data
WHERE secondary_source IS NOT NULL
  AND source_agreement_percentage < 99.0
ORDER BY source_agreement_percentage ASC;
```

**API:** `GET /api/pipeline/data/quality-issues?agreementThreshold=99.0`

---

### 4. Supply Efficiency

```sql
SELECT state,
       AVG((energy_supplied_mu / energy_required_mu) * 100) as efficiency_pct,
       MIN(shortfall_percentage) as best, MAX(shortfall_percentage) as worst
FROM gold_power_data
WHERE energy_required_mu > 0
GROUP BY state ORDER BY efficiency_pct DESC;
```

**Result:** All states maintain >99% supply efficiency

---

### 5. Peak Demand Days

```sql
SELECT state, report_date, energy_required_mu
FROM gold_power_data
WHERE (state, energy_required_mu) IN (
    SELECT state, MAX(energy_required_mu)
    FROM gold_power_data GROUP BY state
)
ORDER BY energy_required_mu DESC;
```

**Result:** All states peaked on Nov 19 (Wednesday)

---

### 6. Data Completeness

```sql
SELECT state, COUNT(DISTINCT report_date) as days_with_data,
       MIN(report_date) as first, MAX(report_date) as last
FROM gold_power_data GROUP BY state;
```

**Result:** 7/7 days for all states (100% complete)

---

## Q&A Agent Examples

### Rule-Based Mode (Default)

```bash
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Which state had the highest shortage?"}'
```

**Response:** "Maharashtra had the highest shortage with 3.50 MU (1.12%) on 2026-11-14"

---

### LLM Mode (AWS Bedrock)

```bash
curl -X POST "http://localhost:8080/api/qa/ask?mode=llm" \
  -H "Content-Type: application/json" \
  -d '{"question": "Compare data quality across all states"}'
```

**Response:** Markdown-formatted analysis with tables, quality metrics, and limitations

---

## Data Limitations

**Period:** 7 days (Nov 13-19, 2026) - too short for seasonal patterns

**Granularity:** Daily state-level - no intraday or city-level data

**Sources:** 
- POSOCO: 1-2 day publication delay
- SLDC: Inconsistent formats across states

**Quality Issues:**
- Minor rounding differences between sources
- Preliminary data (may be corrected later)
- No generation mix (coal/solar/etc.)

---

## Visualization Ideas

1. **Line Chart:** Daily demand by state (shows weekend dips)
2. **Bar Chart:** Average shortfall % by state (color = confidence)
3. **Heatmap:** State × Date with shortage intensity
4. **Time Series:** Supply with confidence bands

---

## Usage Guidance

**✓ Use For:**
- Long-term trend analysis
- State-level capacity planning
- Cross-source validation
- Historical demand modeling

**✗ Don't Use For:**
- Real-time operations (1-2 day delay)
- Intraday patterns (daily aggregates only)
- City/district analysis (state-level only)
- Energy trading (not real-time)

---

## Always Communicate

When presenting this data:

1. **Period:** "Nov 13-19, 2026 (7 days)"
2. **Confidence:** "66.7% HIGH, 33.3% MEDIUM"
3. **Quality:** "Average 0.97/1.00"
4. **Limitation:** "State-level daily aggregates, 1-2 day delay"

**Example Footer:**
> Data Quality: HIGH confidence 66.7%. Based on POSOCO & SLDCs. Period: Nov 13-19, 2026. State-level daily aggregates. See docs for limitations.
