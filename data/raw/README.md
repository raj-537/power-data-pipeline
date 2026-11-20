# Raw Data Samples

This directory contains sample data files downloaded from various sources.

## Files

### POSOCO Data
- `posoco_all_india_2026-11-13_to_2026-11-19.csv`
  - Source: POSOCO Daily Energy Reports (parsed from PDF)
  - Coverage: All states, Nov 13-19, 2026
  - Format: CSV with standardized columns

### State Load Dispatch Center (SLDC) Data

#### Maharashtra SLDC
- `sldc_maharashtra_2026-11-13_to_2026-11-19.csv`
  - Source: https://mahasldc.in/
  - Format: CSV export from daily summary

#### Gujarat SLDC
- `sldc_gujarat_2026-11-13_to_2026-11-19.csv`
  - Source: https://www.sldc.gujarat.gov.in/
  - Format: CSV with peak and average demand

#### Goa Electricity Department
- `sldc_goa_2026-11-13_to_2026-11-19.csv`
  - Source: Goa Electricity Department reports
  - Note: Smaller state, simpler reporting

## Data Notes

1. **Simulated Data**: These are representative samples created for demonstration purposes
2. **Real Implementation**: Would involve parsing PDFs, Excel files, and web scraping
3. **Format Variations**: Note how each SLDC has different column names and structures
4. **Reproducibility**: These files allow the pipeline to run without live internet access

## Observations

- **POSOCO**: Most standardized format, consistent column names
- **Maharashtra SLDC**: Provides both MW and MU values
- **Gujarat SLDC**: Different naming convention, includes peak demand
- **Goa**: Simplest format, smaller volumes

## Date Range

- **Start**: 2026-11-13
- **End**: 2026-11-19  
- **Duration**: 7 days
- **States**: Maharashtra, Gujarat, Goa
