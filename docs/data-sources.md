# Data Sources

## Primary Sources

### 1. POSOCO (Power System Operation Corporation)

**URL:** https://posoco.in/reports/daily-reports/

**Type:** National Load Dispatch Centre (official authority)

**Format:** PDF reports (daily)

**Fields:** State, Date, Requirement (MU), Availability (MU), Shortage (MU), Shortage (%)

**Priority:** 1 (highest)

**Pros:** Authoritative, all states coverage, calculated metrics  
**Cons:** PDF format, 1-2 day delay, no API

---

### 2. State Load Dispatch Centers (SLDCs)

**Examples:**
- Maharashtra: https://mahasldc.in/
- Gujarat: https://www.sldc.gujarat.gov.in/
- Goa: Check state electricity department

**Type:** State-level dispatch centers

**Format:** Excel, PDF, dashboards (varies by state)

**Fields:** Demand (MW), Supply (MW), Deficit (MW), Energy (MU)

**Priority:** 2

**Pros:** State-specific detail, faster updates, validation source  
**Cons:** Inconsistent formats, varying quality, no standard API

---

## Field Mapping

### POSOCO → Target

| Source Field | Target Field | Transform |
|--------------|--------------|-----------|
| "State" | `state` | Standardize name |
| "Requirement (MU)" | `energy_required_mu` | Parse number |
| "Availability (MU)" | `energy_supplied_mu` | Parse number |
| "Shortage (MU)" | `energy_shortfall_mu` | Parse number |
| "Shortage (%)" | `shortfall_percentage` | Parse % |

### SLDC → Target

| Source Field | Target Field | Transform |
|--------------|--------------|-----------|
| State header/filename | `state` | Extract + standardize |
| "Energy Requirement (MU)" | `energy_required_mu` | Parse number |
| "Energy Met (MU)" | `energy_supplied_mu` | Parse number |
| Calculated | `energy_shortfall_mu` | `required - supplied` |
| Calculated | `shortfall_percentage` | `(shortfall / required) × 100` |

---

## Conflict Resolution

When multiple sources exist for same (state, date):

1. Sort by priority (POSOCO=1, SLDC=2)
2. Sort by quality score
3. Pick best, track secondary source
4. Calculate source agreement percentage
5. Flag disagreements >5%

---

## Data Acquisition

**Current:** Manual download → `data/raw/`

**Naming:** `{source}_{state}_{YYYY-MM-DD}.csv`

**Future:** Web scraping (Selenium) + scheduler

---

## Sample Data (Included)

**Period:** 2026-11-13 to 2026-11-19 (7 days)

**States:** Maharashtra, Gujarat, Goa

**Files:**
- `data/raw/posoco_all_india_2026-11-13_to_2026-11-19.csv` (21 records)
- `data/raw/sldc_maharashtra_2026-11-13_to_2026-11-19.csv` (7 records)
- `data/raw/sldc_gujarat_2026-11-13_to_2026-11-19.csv` (7 records)
- `data/raw/sldc_goa_2026-11-13_to_2026-11-19.csv` (7 records)

**Reproducibility:** Pipeline works offline with these samples

---

## Known Quality Issues

- Missing values (some states don't report daily)
- Math inconsistencies (shortage ≠ required - supplied)
- Format variations (PDF table changes)
- State name variations ("DELHI" vs "NCT OF DELHI")
- Negative shortfall (surplus scenarios)

**Mitigation:** Quality scoring + flags at silver layer
